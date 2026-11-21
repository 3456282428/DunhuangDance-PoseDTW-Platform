package com.art.service;

import com.art.dto.VisitorBookingDTO;
import com.art.vo.PortalCourseVO;
import com.art.vo.PortalScheduleVO;

import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface PortalService {
    List<PortalCourseVO> listCourses(Integer categoryId, String ageGroup, String level, String keyword);
    PortalCourseVO getCourseDetail(Integer classId);
    List<PortalScheduleVO> listCourseSchedules(Integer classId);
    void submitTrialBooking(VisitorBookingDTO dto);
    List<VisitorBookingDTO> listMyBookings(String phone);
    List<VisitorBookingDTO> listAllBookings(String status, String keyword);
    void handleBooking(Integer bookingId, String status);

    /** 门户自定义内容：轮播图、主视觉文案、关于我们特色卡片 */
    Object getPortalContent();
}
