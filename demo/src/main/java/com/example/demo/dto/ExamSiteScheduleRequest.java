package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ExamSiteScheduleRequest {
    @NotNull(message = "不能为空")
    private Long examSiteId;

    @NotNull(message = "不能为空")
    private LocalDate examDate;

    @NotBlank(message = "不能为空")
    private String subjectCode;

    @NotNull(message = "不能为空")
    private Integer totalCapacity;

    @NotNull(message = "不能为空")
    private Integer reservedCount;

    public Long getExamSiteId() {
        return examSiteId;
    }

    public void setExamSiteId(Long examSiteId) {
        this.examSiteId = examSiteId;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Integer getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(Integer reservedCount) {
        this.reservedCount = reservedCount;
    }
}
