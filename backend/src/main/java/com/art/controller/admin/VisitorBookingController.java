package com.art.controller.admin;

import com.art.common.Result;
import com.art.dto.VisitorBookingDTO;
import com.art.service.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营管理端 - 游客试听预约（CRM线索管理）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/visitor-booking")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class VisitorBookingController {

    private final PortalService portalService;

    /**
     * 试听线索列表（可按状态/关键词筛选）
     */
    @GetMapping("/list")
    public Result<List<VisitorBookingDTO>> list(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(portalService.listAllBookings(status, keyword));
    }

    /**
     * 处理试听线索（确认 / 取消）
     */
    @PutMapping("/{bookingId}/status")
    public Result<Void> handle(@PathVariable Integer bookingId,
                               @RequestParam String status) {
        portalService.handleBooking(bookingId, status);
        return Result.success();
    }
}
