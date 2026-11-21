package com.art.controller.portal;

import com.art.common.Result;
import com.art.dto.VisitorBookingDTO;
import com.art.service.PortalService;
import com.art.service.StudentProfileService;
import com.art.vo.OrganizationVO;
import com.art.vo.PortalCourseVO;
import com.art.vo.PortalScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 游客门户 - 公开接口（无需登录）
 * 课程浏览、课程详情、试听预约、我的试听记录、机构信息
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;
    private final StudentProfileService profileService;

    /**
     * 课程列表（支持按类别/年龄段/难度/关键词筛选）
     */
    @GetMapping("/courses")
    public Result<List<PortalCourseVO>> listCourses(@RequestParam(required = false) Integer categoryId,
                                                    @RequestParam(required = false) String ageGroup,
                                                    @RequestParam(required = false) String level,
                                                    @RequestParam(required = false) String keyword) {
        return Result.success(portalService.listCourses(categoryId, ageGroup, level, keyword));
    }

    /**
     * 课程详情
     */
    @GetMapping("/course/{classId}")
    public Result<PortalCourseVO> getCourseDetail(@PathVariable Integer classId) {
        return Result.success(portalService.getCourseDetail(classId));
    }

    /**
     * 课程可预约的排班时间（未来待上课）
     */
    @GetMapping("/course/{classId}/schedules")
    public Result<List<PortalScheduleVO>> courseSchedules(@PathVariable Integer classId) {
        return Result.success(portalService.listCourseSchedules(classId));
    }

    /**
     * 提交试听预约（游客填写联系方式即可）
     */
    @PostMapping("/trial")
    public Result<Void> submitTrial(@RequestBody VisitorBookingDTO dto) {
        portalService.submitTrialBooking(dto);
        return Result.success();
    }

    /**
     * 按手机号查询我的试听预约记录
     */
    @GetMapping("/my-bookings")
    public Result<List<VisitorBookingDTO>> myBookings(@RequestParam String phone) {
        return Result.success(portalService.listMyBookings(phone));
    }

    /**
     * 机构公开信息
     */
    @GetMapping("/org")
    public Result<OrganizationVO> orgInfo() {
        return Result.success(profileService.getOrganizationInfo());
    }

    /**
     * 门户自定义内容：轮播图、主视觉文案、关于我们特色卡片
     */
    @GetMapping("/content")
    public Result<Object> portalContent() {
        return Result.success(portalService.getPortalContent());
    }
}
