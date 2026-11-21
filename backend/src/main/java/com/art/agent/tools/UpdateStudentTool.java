package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.dto.UserDTO;
import com.art.entity.SysUser;
import com.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具：修改学员资料（写操作，需用户确认后执行）。
 * 仅支持 STUDENT 角色账号，且只覆盖调用方明确给出的字段，
 * 其余字段沿用原值，避免 updateUser 全量覆盖导致资料丢失。
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class UpdateStudentTool implements AgentTool {

    private final UserService userService;

    @Override
    public String name() { return "update_student"; }

    @Override
    public String description() { return "修改学员资料（姓名/手机号/启用状态），按用户名或userId定位，仅支持学员账号"; }

    @Override
    public String parametersSchema() {
        return "{\"username\":{\"type\":\"string\",\"desc\":\"登录账号，与userId二选一\",\"required\":false},"
                + "\"userId\":{\"type\":\"int\",\"desc\":\"用户ID，与username二选一\",\"required\":false},"
                + "\"realName\":{\"type\":\"string\",\"desc\":\"新姓名（不改则不传）\",\"required\":false},"
                + "\"phone\":{\"type\":\"string\",\"desc\":\"新手机号（不改则不传）\",\"required\":false},"
                + "\"status\":{\"type\":\"string\",\"desc\":\"启用或禁用\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "将修改该学员的资料，修改立即生效。"; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        SysUser target = resolve(args);
        if (target == null) {
            return ToolResult.fail("未找到目标用户", "可先调用 list_users 查询确认用户名或ID");
        }
        if (!"STUDENT".equalsIgnoreCase(target.getRole())) {
            return ToolResult.fail("仅支持修改学员资料",
                    "目标账号角色为 " + target.getRole() + "，请改用用户管理页面操作");
        }
        String realName = ToolArgs.str(args, "realName");
        String phone = ToolArgs.str(args, "phone");
        String status = ToolArgs.str(args, "status");
        if (realName == null && phone == null && status == null) {
            return ToolResult.fail("未指定任何要修改的字段", "请至少提供 realName/phone/status 之一");
        }
        if (realName != null && (realName.length() < 2 || realName.length() > 20)) {
            return ToolResult.fail("姓名长度需2-20位", "请修正后重试");
        }
        Integer newStatus = target.getStatus();
        if (status != null) {
            if (status.matches("(?i)(启用|enable|1)")) {
                newStatus = 1;
            } else if (status.matches("(?i)(禁用|disable|0)")) {
                newStatus = 0;
            } else {
                return ToolResult.fail("status 参数仅支持「启用」或「禁用」", "示例：\"status\":\"禁用\"");
            }
        }
        UserDTO dto = new UserDTO();
        dto.setUsername(target.getUsername());
        dto.setRealName(realName != null ? realName : target.getRealName());
        dto.setPhone(phone != null ? phone : target.getPhone());
        dto.setRole(target.getRole());
        dto.setStatus(newStatus);
        userService.updateUser(target.getUserId(), dto);

        StringBuilder changed = new StringBuilder();
        if (realName != null) changed.append("姓名→").append(realName).append(" ");
        if (phone != null) changed.append("手机→").append(phone).append(" ");
        if (status != null) changed.append("状态→").append(newStatus == 1 ? "启用" : "禁用");
        return ToolResult.ok("已更新学员 " + dto.getRealName() + "（账号 " + target.getUsername() + "）：" + changed,
                Map.of("userId", target.getUserId(), "username", target.getUsername(),
                        "realName", dto.getRealName(), "phone", dto.getPhone(),
                        "status", newStatus == 1 ? "启用" : "禁用"));
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
