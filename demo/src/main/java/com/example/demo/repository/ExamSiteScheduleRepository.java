package com.example.demo.repository;

import com.example.demo.model.ExamSiteSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface ExamSiteScheduleRepository extends JpaRepository<ExamSiteSchedule, Long>, JpaSpecificationExecutor<ExamSiteSchedule> {
    Optional<ExamSiteSchedule> findByExamSiteIdAndExamDateAndSubjectCode(Long examSiteId, LocalDate examDate, String subjectCode);
}
