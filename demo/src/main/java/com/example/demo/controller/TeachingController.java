package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.aop.OpLog;
import com.example.demo.model.CoursePlan;
import com.example.demo.model.TrainingRecord;
import com.example.demo.service.TeachingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teaching")
public class TeachingController {
    private final TeachingService teachingService;

    public TeachingController(TeachingService teachingService) {
        this.teachingService = teachingService;
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('管理员','教练','学员')")
    public ApiResponse<PageResult<CoursePlan>> listPlans(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long studentId,
                                                         @RequestParam(required = false) String coachName) {
        return ApiResponse.ok(teachingService.pagePlans(page, size, studentId, coachName));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAnyRole('管理员','教练','学员')")
    @OpLog(module = "教学", action = "创建课程计划")
    public ApiResponse<CoursePlan> createPlan(@Valid @RequestBody CoursePlan plan) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.ok("创建成功", teachingService.createPlan(plan, auth.getName()));
    }

    @PatchMapping("/plans/{id}/status")
    @PreAuthorize("hasAnyRole('管理员','教练')")
    @OpLog(module = "教学", action = "课程计划状态更新")
    public ApiResponse<CoursePlan> updatePlanStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.ok("状态更新成功", teachingService.updatePlanStatus(id, status));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('管理员','教练')")
    public ApiResponse<PageResult<TrainingRecord>> listRecords(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok(teachingService.pageRecords(page, size, studentId));
    }

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('管理员','教练')")
    @OpLog(module = "教学", action = "记录训练")
    public ApiResponse<TrainingRecord> createRecord(@Valid @RequestBody TrainingRecord record) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.ok("创建成功", teachingService.createRecord(record, auth.getName()));
    }

    @GetMapping("/progress")
    @PreAuthorize("hasAnyRole('管理员','教练','学员')")
    public ApiResponse<Map<String, Object>> progress(@RequestParam Long studentId) {
        return ApiResponse.ok(teachingService.progress(studentId));
    }
}
