package com.example.demo.repository;

import com.example.demo.model.ExamSiteCapacitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamSiteCapacitySnapshotRepository extends JpaRepository<ExamSiteCapacitySnapshot, Long> {
    List<ExamSiteCapacitySnapshot> findByExamSiteIdAndSubjectCodeAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(
            Long examSiteId, String subjectCode, LocalDateTime from, LocalDateTime to);
}
