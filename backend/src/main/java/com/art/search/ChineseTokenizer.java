package com.art.search;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文分词组件：基于 jieba 结巴分词（com.huaban:jieba-analysis，SEARCH 细粒度模式）。
 * 负责把「检索问题 / 知识条目正文」切分为检索词元，并做三类保护：
 * 1) 日期、账号编号、英文缩写等结构化串整体保留（避免 2026-10-16 被切成 2026/10/16 造成误召回）；
 * 2) 停用词、单字虚词过滤（降低噪声、提升准确率）；
 * 3) 分词组件不可用时回退到字符 n-gram，保证检索链路永不中断。
 * @author liukang&&heyan
 */
@Slf4j
@Component
public class ChineseTokenizer {

    /** 结构化串保护：yyyy-MM-dd / yyyyMMdd / 含字母的编号(如 T001、piano-01) / 长度≥6的数字串 */
    private static final Pattern PROTECT = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}|\\d{8}|[A-Za-z]{2,}[A-Za-z0-9_.\\-]*|\\d{6,}");

    /** 中文停用词（高频虚词/疑问词，对检索无区分度） */
    private static final Set<String> STOP = Set.of(
            "的", "了", "是", "在", "我", "你", "他", "她", "它", "们", "这", "那", "个", "和", "与", "及", "或",
            "也", "都", "就", "不", "而", "但", "等", "中", "对", "为", "以", "之", "上", "下", "呢", "吗", "吧",
            "啊", "呀", "哦", "嗯", "给", "把", "被", "让", "从", "到", "着", "过", "来", "去", "很", "还", "又",
            "请问", "请", "帮", "帮我", "帮忙", "一下", "一些", "一个", "什么", "怎么", "怎样", "如何", "哪些",
            "哪个", "哪里", "多少", "几个", "是不是", "有没有", "可以", "能否", "是否", "能不能", "以及", "等等",
            "还有", "然后", "现在", "目前", "我们", "你们", "他们", "这个", "那个", "这些", "那些", "一下下",
            "需要", "想要", "希望", "麻烦", "谢谢", "你好", "您好", "告诉", "看看", "查看", "了解");

    private final JiebaSegmenter segmenter;
    private final boolean available;
    private final Object lock = new Object();

    public ChineseTokenizer() {
        JiebaSegmenter s = null;
        boolean ok = false;
        try {
            s = new JiebaSegmenter();
            // 预热一次，确保词典加载成功（失败则整体降级）
            s.process("分词预热", JiebaSegmenter.SegMode.SEARCH);
            ok = true;
        } catch (Throwable e) {
            log.warn("[中文分词] jieba 分词组件初始化失败，降级为字符 n-gram：{}", e.getMessage());
        }
        this.segmenter = s;
        this.available = ok;
    }

    /** 是否使用专业分词组件（false=降级 n-gram） */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 分词：返回有序去重词元（含结构化串保护 + 停用词过滤）
     * @param text 待切分文本
     */
    public List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Matcher m = PROTECT.matcher(text);
        while (m.find()) {
            String g = m.group().toLowerCase();
            if (g.chars().anyMatch(Character::isLetter)) out.add(g);
            else if (g.length() >= 8) out.add(g); // 纯数字仅保留长串（学号/手机号），避免"2026"泛化命中
        }
        if (available) {
            List<SegToken> segs;
            synchronized (lock) {
                segs = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
            }
            for (SegToken t : segs) {
                String w = t.word == null ? "" : t.word.trim();
                if (w.isEmpty() || STOP.contains(w)) continue;
                if (w.length() < 2 && !w.matches("[a-zA-Z0-9]+")) continue;
                out.add(w.toLowerCase());
            }
        } else {
            out.addAll(ngramFallback(text));
        }
        return new ArrayList<>(out);
    }

    /**
     * 字符 n-gram 特征（供本地向量化使用）：中文按 2-gram 采样，捕捉"排课/排课表"这类近似，
     * 并对整体短语加权，弥补哈希向量对长词不敏感的不足。
     */
    public List<String> bigrams(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        String s = text.replaceAll("[\\s，。！？；：、,.!?;:（）()\"'“”\\[\\]{}<>|]+", "");
        for (int i = 0; i + 2 <= s.length(); i++) {
            String g = s.substring(i, i + 2);
            if (g.chars().allMatch(c -> Character.isLetterOrDigit(c))) out.add(g);
        }
        return out;
    }

    /** 降级：连续中文字符做 2-gram + 结构性串 */
    private List<String> ngramFallback(String text) {
        List<String> out = new ArrayList<>();
        for (String seg : text.split("[\\s，。！？；：、,.!?;:（）()\"'“”]+")) {
            String s = seg.trim();
            if (s.length() < 2) continue;
            out.add(s.toLowerCase());
            for (int i = 0; i + 2 <= s.length(); i++) out.add(s.substring(i, i + 2).toLowerCase());
        }
        return out;
    }
}
