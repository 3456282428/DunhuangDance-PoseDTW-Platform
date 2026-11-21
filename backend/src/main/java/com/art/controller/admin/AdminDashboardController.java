package com.art.controller.admin;

import com.art.common.Result;
import com.art.dto.DashboardOverviewDTO;
import com.art.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营管理端 - 首页概览
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StatisticsService statisticsService;

    /**
     * 获取首页概览数据
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewDTO> getOverview() {
        return Result.success(statisticsService.getDashboardOverview());
    }
}
