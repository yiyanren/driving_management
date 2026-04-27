package com.example.demo.repository;

import com.example.demo.model.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long>, JpaSpecificationExecutor<TrainingRecord> {
    List<TrainingRecord> findByStudentId(Long studentId);

    @Query("select tr.subjectCode, sum(tr.hours) from TrainingRecord tr group by tr.subjectCode")
    List<Object[]> sumHoursGroupBySubjectCode();
}
