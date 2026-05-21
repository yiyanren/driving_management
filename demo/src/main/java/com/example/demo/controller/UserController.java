package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.aop.OpLog;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','招生','教练','学员')")
    public ApiResponse<PageResult<User>> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String role,
                                              @RequestParam(required = false) Long drivingSchoolId) {
        return ApiResponse.ok(userService.page(page, size, role, drivingSchoolId));
    }

    @GetMapping("/me")
    public ApiResponse<User> getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return ApiResponse.ok(userService.getByUsername(username));
    }

    @PutMapping("/me/password")
    @OpLog(module = "用户管理", action = "修改个人密码")
    public ApiResponse<Void> updateMyPassword(@RequestBody Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getByUsername(auth.getName());
        Long userId = user.getId();
        
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        
        userService.updatePassword(userId, oldPassword, newPassword);
        return ApiResponse.ok("密码修改成功", null);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员')")
    @OpLog(module = "用户", action = "新建用户")
    public ApiResponse<User> create(@Valid @RequestBody User user) {
        return ApiResponse.ok("创建成功", userService.create(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员')")
    @OpLog(module = "用户管理", action = "更新用户")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody User payload) {
        return ApiResponse.ok("更新成功", userService.update(id, payload));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员')")
    @OpLog(module = "用户", action = "删除用户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
