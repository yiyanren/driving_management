package com.example.demo.repository;

import com.example.demo.model.ExamSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExamSiteRepository extends JpaRepository<ExamSite, Long>, JpaSpecificationExecutor<ExamSite> {
}
