package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.KbDoc;
import com.art.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：检索机构知识库（三端归集文档统一检索）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool implements AgentTool {

    private final KnowledgeService knowledgeService;

    @Override
    public String name() { return "knowledge_search"; }

    @Override
    public String description() { return "检索机构知识库文档（制度/教案/教学方法/自动归集内容），回答资料类问题前优先调用"; }

    @Override
    public String parametersSchema() {
        return "{\"keyword\":{\"type\":\"string\",\"desc\":\"检索关键词，必填\",\"required\":true},"
                + "\"topK\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大8\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String keyword = ToolArgs.str(args, "keyword");
        if (keyword == null) {
            return ToolResult.fail("缺少检索关键词", "请提供要检索的内容关键词");
        }
        int topK = ToolArgs.limit(args, "topK", 5, 8);
        List<KbDoc> docs = knowledgeService.searchForAi("ADMIN", null, keyword, null, topK);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (KbDoc d : docs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kbId", d.getKbId());
            m.put("sourceType", d.getSourceType());
            m.put("title", d.getTitle());
            m.put("category", d.getCategory());
            // 上下文拼接：摘要过短时附带正文片段，确保价格/课时等正文关键字段可达模型
            String summary = d.getSummary() == null ? "" : d.getSummary();
            String content = d.getContent() == null ? "" : d.getContent().replaceAll("\\s+", " ");
            String brief = summary.length() >= 60 ? summary : summary + " " + content;
            m.put("summary", brief.length() > 200 ? brief.substring(0, 200) + "…" : brief);
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder("知识库命中 ").append(docs.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("sourceType")).append("/").append(r.get("category"))
                    .append("] ").append(r.get("title")).append("：").append(r.get("summary"));
        }
        return ToolResult.ok(sb.toString(), Map.of("total", docs.size(), "rows", rows));
    }
}
