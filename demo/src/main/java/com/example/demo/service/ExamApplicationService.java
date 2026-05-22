package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.ExamApplication;
import com.example.demo.model.ExamSite;
import com.example.demo.repository.ExamApplicationRepository;
import com.example.demo.repository.ExamSiteRepository;
import com.example.demo.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExamApplicationService {
    private final ExamApplicationRepository examApplicationRepository;
    private final StudentRepository studentRepository;
    private final ExamSiteRepository examSiteRepository;

    public ExamApplicationService(ExamApplicationRepository examApplicationRepository,
                                  StudentRepository studentRepository,
                                  ExamSiteRepository examSiteRepository) {
        this.examApplicationRepository = examApplicationRepository;
        this.studentRepository = studentRepository;
        this.examSiteRepository = examSiteRepository;
    }

    public PageResult<ExamApplication> page(int page, int size, Long studentId, String status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<ExamApplication> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (studentId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("studentId"), studentId));
            }
            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
        return PageResult.from(examApplicationRepository.findAll(spec, pageable));
    }

    public ExamApplication get(Long id) {
        return examApplicationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("报考记录不存在"));
    }

    @Transactional
    public ExamApplication create(ExamApplication examApplication) {
        validateForeignKey(examApplication.getStudentId(), examApplication.getExamSiteId());
        ExamSite site = examSiteRepository.findById(examApplication.getExamSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("考场不存在"));
        int cap = site.getCapacity() == null ? 0 : site.getCapacity();
        int used = site.getReservedCount() == null ? 0 : site.getReservedCount();
        if (cap > 0 && used >= cap) {
            throw new IllegalArgumentException("考场场次容量已满");
        }
        site.setReservedCount(used + 1);
        examSiteRepository.save(site);
        if (!StringUtils.hasText(examApplication.getStatus())) {
            examApplication.setStatus("已申请");
        }
        return examApplicationRepository.save(examApplication);
    }

    public ExamApplication updateStatus(Long id, String status) {
        ExamApplication entity = get(id);
        entity.setStatus(status);
        return examApplicationRepository.save(entity);
    }

    private void validateForeignKey(Long studentId, Long examSiteId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("学员不存在");
        }
        if (!examSiteRepository.existsById(examSiteId)) {
            throw new ResourceNotFoundException("考场不存在");
        }
    }
}
