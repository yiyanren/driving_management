package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.aop.OpLog;
import com.example.demo.model.ExamApplication;
import com.example.demo.service.ExamApplicationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exam-applications")
public class ExamApplicationController {
    private final ExamApplicationService examApplicationService;

    public ExamApplicationController(ExamApplicationService examApplicationService) {
        this.examApplicationService = examApplicationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','招生','教练','学员')")
    public ApiResponse<PageResult<ExamApplication>> list(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long studentId,
                                                         @RequestParam(required = false) String status) {
        return ApiResponse.ok(examApplicationService.page(page, size, studentId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生','教练','学员')")
    public ApiResponse<ExamApplication> get(@PathVariable Long id) {
        return ApiResponse.ok(examApplicationService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "报考", action = "创建报考申请")
    public ApiResponse<ExamApplication> create(@Valid @RequestBody ExamApplication examApplication) {
        return ApiResponse.ok("创建成功", examApplicationService.create(examApplication));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "报考", action = "报考审核")
    public ApiResponse<ExamApplication> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.ok("状态更新成功", examApplicationService.updateStatus(id, status));
    }
}
