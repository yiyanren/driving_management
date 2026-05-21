package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_site_schedule", uniqueConstraints = {
        @UniqueConstraint(name = "uk_site_date_subject", columnNames = {"examSiteId", "examDate", "subjectCode"})
})
public class ExamSiteSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @NotNull(message = "不能为空")
    private Integer remainingCount;

    private LocalDateTime syncTime;

    @PrePersist
    @PreUpdate
    public void touch() {
        this.syncTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }

    public LocalDateTime getSyncTime() {
        return syncTime;
    }
}
