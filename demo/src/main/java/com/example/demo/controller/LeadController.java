package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.aop.OpLog;
import com.example.demo.model.Lead;
import com.example.demo.model.Student;
import com.example.demo.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leads")
public class LeadController {
    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','招生','教练')")
    public ApiResponse<PageResult<Lead>> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status) {
        return ApiResponse.ok(leadService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生','教练')")
    public ApiResponse<Lead> get(@PathVariable Long id) {
        return ApiResponse.ok(leadService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "招生", action = "新建线索")
    public ApiResponse<Lead> create(@Valid @RequestBody Lead lead) {
        return ApiResponse.ok("创建成功", leadService.create(lead));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "招生", action = "更新线索")
    public ApiResponse<Lead> update(@PathVariable Long id, @Valid @RequestBody Lead payload) {
        return ApiResponse.ok("更新成功", leadService.update(id, payload));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "招生", action = "线索状态流转")
    public ApiResponse<Lead> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.ok("状态更新成功", leadService.updateStatus(id, status));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "招生", action = "线索转学员")
    public ApiResponse<Student> convertToStudent(@PathVariable Long id, @RequestParam String idCard) {
        return ApiResponse.ok("转换成功", leadService.convertToStudent(id, idCard));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "招生", action = "删除线索")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
