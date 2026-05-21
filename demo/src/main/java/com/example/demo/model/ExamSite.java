package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "exam_site")
public class ExamSite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "不能为空")
    private String name;

    @NotBlank(message = "不能为空")
    private String address;

    private String subjectType;
    private String vehicleTypes;
    private String regionName;
    private Integer capacity;
    private Integer reservedCount;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getVehicleTypes() {
        return vehicleTypes;
    }

    public void setVehicleTypes(String vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(Integer reservedCount) {
        this.reservedCount = reservedCount;
    }
}
