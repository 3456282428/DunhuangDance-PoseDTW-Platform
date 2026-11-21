package com.art.agent;

import java.util.Map;

/** Agent 工具参数解析小工具 */
public final class ToolArgs {

    private ToolArgs() {}

    public static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    public static String str(Map<String, Object> args, String key, String def) {
        String s = str(args, key);
        return s == null ? def : s;
    }

    public static Integer integer(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v).trim()); } catch (Exception e) { return null; }
    }

    public static int limit(Map<String, Object> args, String key, int def, int max) {
        Integer v = integer(args, key);
        if (v == null || v <= 0) return def;
        return Math.min(v, max);
    }
}
