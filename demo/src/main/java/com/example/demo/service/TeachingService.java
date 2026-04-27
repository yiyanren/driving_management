package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.CoursePlan;
import com.example.demo.model.TrainingRecord;
import com.example.demo.repository.CoursePlanRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TrainingRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TeachingService {
    private final CoursePlanRepository coursePlanRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final StudentRepository studentRepository;

    public TeachingService(CoursePlanRepository coursePlanRepository,
                           TrainingRecordRepository trainingRecordRepository,
                           StudentRepository studentRepository) {
        this.coursePlanRepository = coursePlanRepository;
        this.trainingRecordRepository = trainingRecordRepository;
        this.studentRepository = studentRepository;
    }

    public PageResult<CoursePlan> pagePlans(int page, int size, Long studentId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<CoursePlan> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (studentId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("studentId"), studentId));
            }
            return predicate;
        };
        return PageResult.from(coursePlanRepository.findAll(spec, pageable));
    }

    public CoursePlan createPlan(CoursePlan plan) {
        validateStudent(plan.getStudentId());
        return coursePlanRepository.save(plan);
    }

    public PageResult<TrainingRecord> pageRecords(int page, int size, Long studentId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<TrainingRecord> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (studentId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("studentId"), studentId));
            }
            return predicate;
        };
        return PageResult.from(trainingRecordRepository.findAll(spec, pageable));
    }

    public TrainingRecord createRecord(TrainingRecord record) {
        validateStudent(record.getStudentId());
        return trainingRecordRepository.save(record);
    }

    public Map<String, Object> progress(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        validateStudent(studentId);
        Map<String, BigDecimal> subjectHours = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (TrainingRecord r : trainingRecordRepository.findByStudentId(studentId)) {
            BigDecimal h = r.getHours() == null ? BigDecimal.ZERO : r.getHours();
            total = total.add(h);
            subjectHours.put(r.getSubjectCode(), subjectHours.getOrDefault(r.getSubjectCode(), BigDecimal.ZERO).add(h));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("subjectHours", subjectHours);
        result.put("totalHours", total);
        result.put("readyForExam", total.compareTo(new BigDecimal("30")) >= 0);
        return result;
    }

    private void validateStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("学员不存在");
        }
    }
}
