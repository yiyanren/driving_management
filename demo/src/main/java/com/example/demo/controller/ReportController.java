package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(required = false) LocalDate from,
                                                     @RequestParam(required = false) LocalDate to) {
        return ApiResponse.ok(reportService.overview(from, to));
    }

    @GetMapping("/funnel")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    public ApiResponse<Map<String, Object>> funnel(@RequestParam(required = false) LocalDate from,
                                                   @RequestParam(required = false) LocalDate to) {
        return ApiResponse.ok(reportService.funnel(from, to));
    }

    @GetMapping("/export/overview")
    @PreAuthorize("hasAnyRole('管理员','招生')")
    public ResponseEntity<String> exportOverview(@RequestParam(required = false) LocalDate from,
                                                 @RequestParam(required = false) LocalDate to) {
        String csv = reportService.overviewCsv(from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overview_report.csv")
                .body(csv);
    }
}
