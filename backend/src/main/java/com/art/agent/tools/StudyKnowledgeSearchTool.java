package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.KbDoc;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：检索学习知识库（学生端，范围=本人条目+机构共享条目）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class StudyKnowledgeSearchTool implements AgentTool {

    private final KnowledgeService knowledgeService;

    @Override
    public String name() { return "study_knowledge_search"; }

    @Override
    public String description() { return "检索学习知识库（练习方法/基本功指南/机构共享资料），回答学习资料类问题前优先调用"; }

    @Override
    public String parametersSchema() {
        return "{\"keyword\":{\"type\":\"string\",\"desc\":\"检索关键词，必填\",\"required\":true},"
                + "\"topK\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大8\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String keyword = ToolArgs.str(args, "keyword");
        if (keyword == null) return ToolResult.fail("缺少检索关键词", "请提供要检索的内容关键词");
        int topK = ToolArgs.limit(args, "topK", 5, 8);
        Integer studentId = SecurityUtil.getCurrentStudentId();
        List<KbDoc> docs = knowledgeService.searchForAi("STUDENT", studentId, keyword, null, topK);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (KbDoc d : docs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kbId", d.getKbId());
            m.put("sourceType", d.getSourceType());
            m.put("title", d.getTitle());
            m.put("category", d.getCategory());
            String summary = d.getSummary() == null ? "" : d.getSummary();
            m.put("summary", summary.length() > 120 ? summary.substring(0, 120) + "…" : summary);
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("学习知识库未命中相关内容。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("学习知识库命中 ").append(docs.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("sourceType")).append("/").append(r.get("category"))
                    .append("] ").append(r.get("title")).append("：").append(r.get("summary"));
        }
        return ToolResult.ok(sb.toString(), Map.of("total", docs.size(), "rows", rows));
    }
}
