package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "不能为空")
    private String name;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "格式不正确")
    private String phone;

    @NotBlank(message = "不能为空")
    private String idCard;

    @NotBlank(message = "不能为空")
    private String status;

    private Boolean subjectOnePassed;
    private Boolean subjectTwoPassed;
    private Boolean subjectThreePassed;
    private Boolean subjectFourPassed;

    private Long drivingSchoolId;

    private String licenseType;

    private Long userId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (subjectOnePassed == null) subjectOnePassed = false;
        if (subjectTwoPassed == null) subjectTwoPassed = false;
        if (subjectThreePassed == null) subjectThreePassed = false;
        if (subjectFourPassed == null) subjectFourPassed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        if (subjectOnePassed == null) subjectOnePassed = false;
        if (subjectTwoPassed == null) subjectTwoPassed = false;
        if (subjectThreePassed == null) subjectThreePassed = false;
        if (subjectFourPassed == null) subjectFourPassed = false;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getSubjectOnePassed() {
        return subjectOnePassed;
    }

    public void setSubjectOnePassed(Boolean subjectOnePassed) {
        this.subjectOnePassed = subjectOnePassed;
    }

    public Boolean getSubjectTwoPassed() {
        return subjectTwoPassed;
    }

    public void setSubjectTwoPassed(Boolean subjectTwoPassed) {
        this.subjectTwoPassed = subjectTwoPassed;
    }

    public Boolean getSubjectThreePassed() {
        return subjectThreePassed;
    }

    public void setSubjectThreePassed(Boolean subjectThreePassed) {
        this.subjectThreePassed = subjectThreePassed;
    }

    public Boolean getSubjectFourPassed() {
        return subjectFourPassed;
    }

    public void setSubjectFourPassed(Boolean subjectFourPassed) {
        this.subjectFourPassed = subjectFourPassed;
    }

    public Long getDrivingSchoolId() {
        return drivingSchoolId;
    }

    public void setDrivingSchoolId(Long drivingSchoolId) {
        this.drivingSchoolId = drivingSchoolId;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
