package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.BusinessException;
import com.art.entity.KbDoc;
import com.art.entity.KbRelation;
import com.art.mapper.KbRelationMapper;
import com.art.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具：知识库治理（Agent 守规管理知识库，确保条目清晰、不被污染）。
 * <p>
 * 治理规则（可追溯）：
 * <ul>
 *   <li>扫描项：完全重复标题、缺失标签、标题无【分类】规范前缀、有业务引用但无任何关联的孤立条目；</li>
 *   <li>fix 模式：仅做「规范化」修复——补全标签（分类+标题前缀+治理标记）、规范标题前缀，全部写入操作日志；</li>
 *   <li>重复清理：仅当 removeDuplicates=true 时执行，只删除「标题完全相同」的重复项，
 *       每组保留创建时间最早的一条，且只处理手动条目（AUTO 归集条目由业务系统管理，不动删）；</li>
 *   <li>执行前需用户确认（写操作门控），非管理员角色禁止调用。</li>
 * </ul>
 * @author liukang&&heyan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KbCurateTool implements AgentTool {

    private final KnowledgeService knowledgeService;
    private final KbRelationMapper relationMapper;

    /** 规范标题前缀形态：【分类】xxx */
    private static final Pattern TITLE_PREFIX = Pattern.compile("^【[^】]{1,20}】");
    /** 业务引用规范形态：前缀:数字 */
    private static final Pattern REF_PATTERN = Pattern.compile("^([a-z_]+:\\d+)");

    @Override
    public String name() { return "kb_curate"; }

    @Override
    public String description() {
        return "知识库治理：扫描三端知识库异常条目（完全重复标题/缺失标签/标题无规范前缀/孤立无关联），"
                + "可自动规范化修复并清理重复条目，确保知识库条目清晰、不被污染";
    }

    @Override
    public String parametersSchema() {
        return "{\"scope\":{\"type\":\"string\",\"desc\":\"知识库端别：ADMIN/TEACHER/STUDENT，默认ADMIN\",\"required\":false},"
                + "\"mode\":{\"type\":\"string\",\"desc\":\"scan仅扫描报告（默认）/fix扫描并自动规范化修复\",\"required\":false},"
                + "\"removeDuplicates\":{\"type\":\"string\",\"desc\":\"fix模式下是否删除完全重复条目（true/false，默认false，仅删手动条目且保留最早一条）\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() {
        return "知识库治理是写操作：fix 模式将自动补全标签、规范标题前缀；若开启重复清理，将删除「标题完全相同」的手动条目（每组保留最早创建的一条）。所有改动均写入知识库操作日志，可追溯。";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String scope = ToolArgs.str(args, "scope", "ADMIN").toUpperCase();
        try {
            knowledgeService.checkScope(scope);
        } catch (BusinessException e) {
            return ToolResult.fail("非法的知识库范围：" + scope, "scope 只能是 ADMIN/TEACHER/STUDENT");
        }
        boolean fix = "fix".equalsIgnoreCase(ToolArgs.str(args, "mode", "scan"));
        boolean removeDuplicates = fix && isTrue(ToolArgs.str(args, "removeDuplicates"));

        // 1) 全量扫描该端条目（课程设计数据规模下单页取回）
        List<KbDoc> docs = knowledgeService.pageDocs(scope, 1, 5000, null, null, null, null).getRows();

        // 2) 异常识别
        Map<String, List<KbDoc>> byTitle = new LinkedHashMap<>();
        List<KbDoc> noTag = new ArrayList<>();
        List<KbDoc> noPrefix = new ArrayList<>();
        for (KbDoc d : docs) {
            byTitle.computeIfAbsent(d.getTitle() == null ? "" : d.getTitle(), k -> new ArrayList<>()).add(d);
            if (d.getTags() == null || d.getTags().isBlank()) noTag.add(d);
            if (d.getTitle() == null || !TITLE_PREFIX.matcher(d.getTitle()).find()) noPrefix.add(d);
        }
        List<Map<String, Object>> dupGroups = new ArrayList<>();
        for (List<KbDoc> group : byTitle.values()) {
            if (group.size() < 2) continue;
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("title", group.get(0).getTitle());
            g.put("count", group.size());
            g.put("kbIds", group.stream().map(KbDoc::getKbId).toList());
            dupGroups.add(g);
        }
        // 孤立条目：有业务引用但关系表中无任何关联（一次装载全部关系后内存判定）
        List<Integer> orphans = new ArrayList<>();
        try {
            Set<String> connected = new HashSet<>();
            for (KbRelation r : relationMapper.selectAll()) {
                if (r.getSrcRef() != null) connected.add(r.getSrcRef());
                if (r.getDstRef() != null) connected.add(r.getDstRef());
            }
            for (KbDoc d : docs) {
                if (d.getSourceRef() == null || d.getSourceRef().isBlank()) continue;
                if (!connected.contains(canonical(d.getSourceRef()))) orphans.add(d.getKbId());
            }
        } catch (Exception e) {
            log.warn("[kb_curate] 孤立条目检测失败：{}", e.getMessage());
        }

        // 3) fix 模式：自动规范化（补标签 + 规范标题前缀），重复清理需显式开启
        List<Map<String, Object>> removed = new ArrayList<>();
        int fixedTags = 0, fixedTitles = 0;
        if (fix) {
            for (KbDoc d : noTag) {
                try {
                    KbDoc patch = new KbDoc();
                    patch.setKbId(d.getKbId());
                    patch.setTags(deriveTags(d));
                    knowledgeService.updateDoc(scope, d.getKbId(), patch, null);
                    fixedTags++;
                } catch (Exception e) {
                    log.warn("[kb_curate] 补全标签失败 kbId={}：{}", d.getKbId(), e.getMessage());
                }
            }
            for (KbDoc d : noPrefix) {
                try {
                    KbDoc patch = new KbDoc();
                    patch.setKbId(d.getKbId());
                    String category = d.getCategory() == null || d.getCategory().isBlank() ? "综合" : d.getCategory();
                    patch.setTitle("【" + category + "】" + d.getTitle());
                    knowledgeService.updateDoc(scope, d.getKbId(), patch, null);
                    fixedTitles++;
                } catch (Exception e) {
                    log.warn("[kb_curate] 规范标题失败 kbId={}：{}", d.getKbId(), e.getMessage());
                }
            }
            if (removeDuplicates) {
                for (List<KbDoc> group : byTitle.values()) {
                    if (group.size() < 2) continue;
                    // 保留创建时间最早的一条；只清理手动条目，AUTO 归集条目由业务系统管理不动删
                    List<KbDoc> manual = group.stream()
                            .filter(d -> d.getSourceType() == null || !d.getSourceType().startsWith("AUTO"))
                            .sorted((a, b) -> {
                                LocalDateTime ta = a.getCreateTime() == null ? LocalDateTime.MAX : a.getCreateTime();
                                LocalDateTime tb = b.getCreateTime() == null ? LocalDateTime.MAX : b.getCreateTime();
                                return ta.compareTo(tb);
                            }).toList();
                    // 组内手动条目数 > 1 才有可清理的重复
                    Set<Integer> keep = new LinkedHashSet<>();
                    if (!manual.isEmpty()) keep.add(manual.get(0).getKbId());
                    for (int i = (manual.isEmpty() ? 0 : 1); i < manual.size(); i++) {
                        KbDoc dup = manual.get(i);
                        if (keep.contains(dup.getKbId())) continue;
                        try {
                            knowledgeService.deleteDoc(scope, dup.getKbId(), null);
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("kbId", dup.getKbId());
                            item.put("title", dup.getTitle());
                            item.put("createTime", dup.getCreateTime());
                            removed.add(item);
                        } catch (Exception e) {
                            log.warn("[kb_curate] 删除重复条目失败 kbId={}：{}", dup.getKbId(), e.getMessage());
                        }
                    }
                }
            }
        }

        // 4) 结构化治理报告
        StringBuilder sb = new StringBuilder("知识库治理报告（scope=").append(scope).append("，mode=").append(fix ? "fix" : "scan").append("）：")
                .append("\n扫描条目：").append(docs.size())
                .append("\n· 完全重复标题：").append(dupGroups.size()).append(" 组")
                .append("\n· 缺失标签：").append(noTag.size()).append(" 条").append(fix ? "（已补全 " + fixedTags + " 条）" : "")
                .append("\n· 标题无规范前缀【分类】：").append(noPrefix.size()).append(" 条").append(fix ? "（已规范 " + fixedTitles + " 条）" : "")
                .append("\n· 孤立条目（有业务引用但无关联）：").append(orphans.size()).append(" 条（关联随归集自动重建，无需处理）");
        if (removeDuplicates) {
            sb.append("\n· 已清理重复条目：").append(removed.size()).append(" 条（每组保留创建时间最早的一条）");
        }
        sb.append(fix ? "\n所有修复与清理均已写入知识库操作日志，可追溯。" : "\n当前为 scan 模式：仅报告未改动；如需自动规范化修复请用 mode=fix。");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", scope);
        data.put("mode", fix ? "fix" : "scan");
        data.put("scanned", docs.size());
        data.put("duplicateGroups", dupGroups);
        data.put("noTagKbIds", noTag.stream().map(KbDoc::getKbId).toList());
        data.put("noPrefixKbIds", noPrefix.stream().map(KbDoc::getKbId).toList());
        data.put("orphanKbIds", orphans);
        data.put("fixedTags", fixedTags);
        data.put("fixedTitles", fixedTitles);
        data.put("removed", removed);
        data.put("removeDuplicates", removeDuplicates);
        return ToolResult.ok(sb.toString(), data);
    }

    /** 派生标签：分类 + 标题前缀语义 + 治理标记，保证条目可检索可分类 */
    private String deriveTags(KbDoc d) {
        Set<String> tags = new LinkedHashSet<>();
        if (d.getCategory() != null && !d.getCategory().isBlank()) tags.add(d.getCategory());
        if (d.getTitle() != null) {
            Matcher m = TITLE_PREFIX.matcher(d.getTitle());
            if (m.find()) tags.add(m.group().replace("【", "").replace("】", ""));
        }
        tags.add("治理补全");
        return String.join(",", tags);
    }

    private boolean isTrue(String v) {
        return v != null && ("true".equalsIgnoreCase(v) || "1".equals(v) || "是".equals(v) || "yes".equalsIgnoreCase(v));
    }

    /** 业务引用规范化：schedule:12:t3 → schedule:12 */
    private String canonical(String ref) {
        Matcher m = REF_PATTERN.matcher(ref);
        return m.find() ? m.group(1) : ref.trim();
    }
}
