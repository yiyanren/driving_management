package com.example.demo.repository;

import com.example.demo.model.ExamApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExamApplicationRepository extends JpaRepository<ExamApplication, Long>, JpaSpecificationExecutor<ExamApplication> {
    List<ExamApplication> findByStudentId(Long studentId);

    @Query("select ea.status, count(ea) from ExamApplication ea group by ea.status")
    List<Object[]> countGroupByStatus();
}
