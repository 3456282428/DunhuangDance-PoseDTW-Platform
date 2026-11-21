package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.Course;
import com.art.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具：管理端新建课程产品（写操作，需用户确认后执行）。
 * 课程编号由系统自动生成，不暴露 courseNo 参数。
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class CreateCourseTool implements AgentTool {

    private final CourseService courseService;

    @Override
    public String name() { return "create_course"; }

    @Override
    public String description() { return "新建一门课程产品（课程名称+类别+价格课时等），课程编号自动生成，默认上架"; }

    @Override
    public String parametersSchema() {
        return "{\"courseName\":{\"type\":\"string\",\"desc\":\"课程名称（全局唯一）\",\"required\":true},"
                + "\"category\":{\"type\":\"string\",\"desc\":\"课程类别，须与系统现有类别一致，如 舞蹈/乐器/美术/声乐\",\"required\":true},"
                + "\"price\":{\"type\":\"number\",\"desc\":\"优惠售价（元）\",\"required\":true},"
                + "\"originalPrice\":{\"type\":\"number\",\"desc\":\"原价（元），不填则等于售价\",\"required\":false},"
                + "\"totalHours\":{\"type\":\"integer\",\"desc\":\"总课时数\",\"required\":false},"
                + "\"lessonDuration\":{\"type\":\"integer\",\"desc\":\"单节时长（分钟），默认45\",\"required\":false},"
                + "\"ageGroup\":{\"type\":\"string\",\"desc\":\"适龄段，如 4-6岁/7-12岁/成人\",\"required\":false},"
                + "\"level\":{\"type\":\"string\",\"desc\":\"难度级别，如 启蒙/初级/中级/高级\",\"required\":false},"
                + "\"intro\":{\"type\":\"string\",\"desc\":\"课程简介\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "将在课程产品库中新建一门课程并默认上架，所有学员端立即可见，优惠价不能高于原价。"; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String courseName = ToolArgs.str(args, "courseName");
        String category = ToolArgs.str(args, "category");
        String priceStr = ToolArgs.str(args, "price");
        if (courseName == null || category == null || priceStr == null) {
            return ToolResult.fail("缺少必填参数（courseName/category/price）",
                    "请向用户确认课程名称、类别与售价后再创建");
        }
        BigDecimal price;
        BigDecimal originalPrice;
        try {
            price = new BigDecimal(priceStr);
            String op = ToolArgs.str(args, "originalPrice");
            originalPrice = op == null ? price : new BigDecimal(op);
        } catch (NumberFormatException e) {
            return ToolResult.fail("价格格式不正确", "请提供数字金额，例如 2999 或 2999.00");
        }
        if (price.signum() <= 0 || originalPrice.signum() <= 0) {
            return ToolResult.fail("价格必须大于0", "请核对售价与原价");
        }
        if (price.compareTo(originalPrice) > 0) {
            return ToolResult.fail("优惠价不能高于原价",
                    "当前售价 " + price + " 元，原价 " + originalPrice + " 元");
        }

        Course c = new Course();
        c.setCourseName(courseName);
        c.setCategory(category);
        c.setPrice(price);
        c.setOriginalPrice(originalPrice);
        c.setTotalHours(ToolArgs.integer(args, "totalHours"));
        Integer lessonDuration = ToolArgs.integer(args, "lessonDuration");
        c.setLessonDuration(lessonDuration);
        c.setAgeGroup(ToolArgs.str(args, "ageGroup"));
        c.setLevel(ToolArgs.str(args, "level"));
        c.setIntro(ToolArgs.str(args, "intro"));
        courseService.create(c);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", c.getCourseId());
        data.put("courseNo", c.getCourseNo());
        data.put("courseName", c.getCourseName());
        data.put("category", c.getCategory());
        data.put("categoryMatched", c.getCategoryId() != null);
        data.put("price", String.valueOf(c.getPrice()));
        data.put("originalPrice", String.valueOf(c.getOriginalPrice()));
        data.put("lessonDuration", c.getLessonDuration());
        data.put("status", "上架");
        return ToolResult.ok("已新建课程《" + courseName + "》（编号 " + c.getCourseNo()
                + "，类别 " + category + "，售价 " + price + " 元，已上架）", data);
    }
}
