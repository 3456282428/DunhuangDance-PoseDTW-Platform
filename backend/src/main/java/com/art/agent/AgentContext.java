package com.art.agent;

import java.util.List;

/**
 * Agent 工具调用上下文：携带当前操作者身份与角色，
 * 供工具执行层做权限校验与数据范围控制。
 * @author liukang&&heyan
 */
public record AgentContext(Integer userId, String username, List<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }
}
