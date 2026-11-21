package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.SysUser;
import com.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具：删除用户（高危操作，需用户确认后执行）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class DeleteUserTool implements AgentTool {

    private final UserService userService;

    @Override
    public String name() { return "delete_user"; }

    @Override
    public String description() { return "删除指定用户账号（按用户名或userId定位），删除后不可恢复"; }

    @Override
    public String parametersSchema() {
        return "{\"username\":{\"type\":\"string\",\"desc\":\"登录账号，与userId二选一\",\"required\":false},"
                + "\"userId\":{\"type\":\"int\",\"desc\":\"用户ID，与username二选一\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "删除操作不可恢复，该用户的账号与关联身份将被移除。"; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String username = ToolArgs.str(args, "username");
        SysUser target = username != null ? userService.findByUsername(username) : null;
        if (target == null) {
            Integer userId = ToolArgs.integer(args, "userId");
            if (userId != null) {
                var page = userService.pageUsers(1, 200, null, null, null);
                target = page.getRows().stream().filter(u -> u.getUserId().equals(userId)).findFirst().orElse(null);
            }
        }
        if (target == null) {
            return ToolResult.fail("未找到目标用户", "可先调用 list_users 查询确认用户名或ID");
        }
        if (target.getUserId().equals(ctx.userId())) {
            return ToolResult.fail("不能删除当前登录的管理员账号", "如需调整请使用其他管理员账号操作");
        }
        userService.deleteUser(target.getUserId());
        return ToolResult.ok("已删除用户 " + target.getRealName() + "（" + target.getUsername() + "，" + target.getRole() + "）",
                Map.of("userId", target.getUserId(), "username", target.getUsername()));
    }
}
