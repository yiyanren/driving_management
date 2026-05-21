package com.example.demo.repository;

import com.example.demo.model.ExamSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ExamSiteRepository extends JpaRepository<ExamSite, Long>, JpaSpecificationExecutor<ExamSite> {
    Optional<ExamSite> findFirstByNameAndSubjectTypeAndAddress(String name, String subjectType, String address);

    List<ExamSite> findByIdIn(List<Long> ids);
}
