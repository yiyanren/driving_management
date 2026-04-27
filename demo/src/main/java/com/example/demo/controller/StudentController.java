package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.aop.OpLog;
import com.example.demo.model.Student;
import com.example.demo.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','招生','教练')")
    public ApiResponse<PageResult<Student>> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.ok(studentService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生','教练','学员')")
    public ApiResponse<Student> get(@PathVariable Long id) {
        return ApiResponse.ok(studentService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "学员", action = "新建学员")
    public ApiResponse<Student> create(@Valid @RequestBody Student student) {
        return ApiResponse.ok("创建成功", studentService.create(student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    @OpLog(module = "学员", action = "更新学员")
    public ApiResponse<Student> update(@PathVariable Long id, @Valid @RequestBody Student payload) {
        return ApiResponse.ok("更新成功", studentService.update(id, payload));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('管理员','招生','教练')")
    @OpLog(module = "学员", action = "学员状态流转")
    public ApiResponse<Student> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.ok("状态更新成功", studentService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员')")
    @OpLog(module = "学员", action = "删除学员")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
