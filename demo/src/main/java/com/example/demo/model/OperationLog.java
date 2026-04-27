package com.example.demo.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_log")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String moduleName;
    private String actionName;
    private String operatorName;
    private String result;

    @Column(length = 1000)
    private String detail;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
