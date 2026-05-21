package com.example.demo.repository;

import com.example.demo.model.DrivingSchool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DrivingSchoolRepository extends JpaRepository<DrivingSchool, Long>, JpaSpecificationExecutor<DrivingSchool> {
}