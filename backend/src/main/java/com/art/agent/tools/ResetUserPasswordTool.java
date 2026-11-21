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
 * 工具：重置用户密码（敏感操作，需用户确认后执行）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ResetUserPasswordTool implements AgentTool {

    private final UserService userService;

    @Override
    public String name() { return "reset_user_password"; }

    @Override
    public String description() { return "将指定用户的登录密码重置为新密码（默认123456），按用户名或userId定位用户"; }

    @Override
    public String parametersSchema() {
        return "{\"username\":{\"type\":\"string\",\"desc\":\"登录账号，与userId二选一\",\"required\":false},"
                + "\"userId\":{\"type\":\"int\",\"desc\":\"用户ID，与username二选一\",\"required\":false},"
                + "\"newPassword\":{\"type\":\"string\",\"desc\":\"新密码，默认123456\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "重置后该用户原密码立即失效，只能用新密码登录。"; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        SysUser target = resolve(args);
        if (target == null) {
            return ToolResult.fail("未找到目标用户", "可先调用 list_users 查询确认用户名或ID");
        }
        if ("ADMIN".equalsIgnoreCase(target.getRole()) && target.getUserId().equals(ctx.userId())) {
            return ToolResult.fail("不能通过Agent重置自己的密码", "请走个人信息页修改密码");
        }
        String pwd = ToolArgs.str(args, "newPassword", "123456");
        if (pwd.length() < 6) {
            return ToolResult.fail("新密码长度至少6位", "请设置更长的新密码后重试");
        }
        userService.resetPassword(target.getUserId(), pwd);
        return ToolResult.ok("已将用户 " + target.getRealName() + "（" + target.getUsername() + "）的密码重置",
                Map.of("userId", target.getUserId(), "username", target.getUsername()));
    }

    private SysUser resolve(Map<String, Object> args) {
        String username = ToolArgs.str(args, "username");
        if (username != null) return userService.findByUsername(username);
        Integer userId = ToolArgs.integer(args, "userId");
        if (userId != null) {
            var page = userService.pageUsers(1, 200, null, null, null);
            return page.getRows().stream().filter(u -> u.getUserId().equals(userId)).findFirst().orElse(null);
        }
        return null;
    }
}
