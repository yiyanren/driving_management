package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.model.DrivingSchool;
import com.example.demo.service.DrivingSchoolService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driving-schools")
public class DrivingSchoolController {

    private final DrivingSchoolService drivingSchoolService;

    public DrivingSchoolController(DrivingSchoolService drivingSchoolService) {
        this.drivingSchoolService = drivingSchoolService;
    }

    @GetMapping
    public ApiResponse<PageResult<DrivingSchool>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(drivingSchoolService.page(page, size, keyword));
    }

    @GetMapping("/all")
    public ApiResponse<List<DrivingSchool>> listAll() {
        return ApiResponse.ok(drivingSchoolService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<DrivingSchool> get(@PathVariable Long id) {
        return ApiResponse.ok(drivingSchoolService.get(id));
    }

    @PostMapping
    public ApiResponse<DrivingSchool> create(@Valid @RequestBody DrivingSchool drivingSchool) {
        return ApiResponse.ok(drivingSchoolService.create(drivingSchool));
    }

    @PutMapping("/{id}")
    public ApiResponse<DrivingSchool> update(@PathVariable Long id, @Valid @RequestBody DrivingSchool drivingSchool) {
        return ApiResponse.ok(drivingSchoolService.update(id, drivingSchool));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        drivingSchoolService.delete(id);
        return ApiResponse.ok(null);
    }
}