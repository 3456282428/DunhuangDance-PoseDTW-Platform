package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.dto.UserDTO;
import com.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具：新增学员账号（写操作，需用户确认后执行）。
 * 角色固定为 STUDENT，不暴露 role 参数，防止借 Agent 提权创建管理账号。
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class CreateStudentTool implements AgentTool {

    private final UserService userService;

    @Override
    public String name() { return "create_student"; }

    @Override
    public String description() { return "新增一个学员账号（登录名+姓名+手机号），初始密码默认123456"; }

    @Override
    public String parametersSchema() {
        return "{\"username\":{\"type\":\"string\",\"desc\":\"登录账号（全局唯一，3-20位）\",\"required\":true},"
                + "\"realName\":{\"type\":\"string\",\"desc\":\"学员姓名\",\"required\":true},"
                + "\"phone\":{\"type\":\"string\",\"desc\":\"11位手机号\",\"required\":true},"
                + "\"password\":{\"type\":\"string\",\"desc\":\"初始密码（至少6位），默认123456\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "将在系统中新建一个学员账号，新账号用初始密码即可登录。"; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String username = ToolArgs.str(args, "username");
        String realName = ToolArgs.str(args, "realName");
        String phone = ToolArgs.str(args, "phone");
        String password = ToolArgs.str(args, "password", "123456");
        if (username == null || realName == null || phone == null) {
            return ToolResult.fail("缺少必填参数（username/realName/phone）", "请向用户确认账号、姓名与手机号后再创建");
        }
        if (username.length() < 3 || username.length() > 20) {
            return ToolResult.fail("登录账号长度需3-20位", "当前：" + username.length() + "位");
        }
        if (password.length() < 6) {
            return ToolResult.fail("初始密码长度至少6位", "可省略密码参数，使用默认123456");
        }
        if (userService.findByUsername(username) != null) {
            return ToolResult.fail("登录账号「" + username + "」已存在",
                    "请更换账号名；若本就是要改既有账号资料，请改用 update_student");
        }
        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setRole("STUDENT");
        dto.setRealName(realName);
        dto.setPhone(phone);
        dto.setStatus(1);
        userService.createUser(dto);
        boolean defaultPwd = password.equals("123456");
        return ToolResult.ok("已新增学员 " + realName + "（账号 " + username + "，手机 " + phone
                        + "，初始密码" + (defaultPwd ? "123456" : "已按指定设置") + "）",
                Map.of("username", username, "realName", realName,
                        "phone", phone, "role", "STUDENT", "status", "启用"));
    }
}
