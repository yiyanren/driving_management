package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.aop.OpLog;
import com.example.demo.model.ExamSite;
import com.example.demo.service.ExamSiteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-sites")
public class ExamSiteController {
    private final ExamSiteService examSiteService;

    public ExamSiteController(ExamSiteService examSiteService) {
        this.examSiteService = examSiteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','招生','教练','学员','STUDENT')")
    public ApiResponse<PageResult<ExamSite>> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(examSiteService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生','教练','学员','STUDENT')")
    public ApiResponse<ExamSite> get(@PathVariable Long id) {
        return ApiResponse.ok(examSiteService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "考场", action = "创建考场")
    public ApiResponse<ExamSite> create(@Valid @RequestBody ExamSite examSite) {
        return ApiResponse.ok("创建成功", examSiteService.create(examSite));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "考场", action = "导入真实考场")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok("导入成功", examSiteService.importExcel(file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "考场", action = "更新考场")
    public ApiResponse<ExamSite> update(@PathVariable Long id, @Valid @RequestBody ExamSite payload) {
        return ApiResponse.ok("更新成功", examSiteService.update(id, payload));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员')")
    @OpLog(module = "考场", action = "删除考场")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examSiteService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
