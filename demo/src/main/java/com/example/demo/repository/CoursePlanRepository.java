package com.example.demo.repository;

import com.example.demo.model.CoursePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CoursePlanRepository extends JpaRepository<CoursePlan, Long>, JpaSpecificationExecutor<CoursePlan> {
    List<CoursePlan> findByStudentId(Long studentId);
}
