package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.Lead;
import com.example.demo.model.Student;
import com.example.demo.repository.LeadRepository;
import com.example.demo.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final StudentRepository studentRepository;

    public LeadService(LeadRepository leadRepository, StudentRepository studentRepository) {
        this.leadRepository = leadRepository;
        this.studentRepository = studentRepository;
    }

    public PageResult<Lead> page(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<Lead> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(keyword)) {
                var p = cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("phone"), "%" + keyword + "%"),
                        cb.like(root.get("source"), "%" + keyword + "%")
                );
                predicate = cb.and(predicate, p);
            }
            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
        return PageResult.from(leadRepository.findAll(spec, pageable));
    }

    public Lead get(Long id) {
        return leadRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("意向客户不存在"));
    }

    public Lead create(Lead lead) {
        if (!StringUtils.hasText(lead.getStatus())) {
            lead.setStatus("新建");
        }
        return leadRepository.save(lead);
    }

    public Lead update(Long id, Lead payload) {
        Lead lead = get(id);
        lead.setName(payload.getName());
        lead.setPhone(payload.getPhone());
        lead.setSource(payload.getSource());
        lead.setOwner(payload.getOwner());
        lead.setStatus(payload.getStatus());
        return leadRepository.save(lead);
    }

    public Lead updateStatus(Long id, String status) {
        Lead lead = get(id);
        lead.setStatus(status);
        return leadRepository.save(lead);
    }

    public Student convertToStudent(Long id, String idCard) {
        Lead lead = get(id);
        if (studentRepository.existsByPhone(lead.getPhone())) {
            throw new IllegalArgumentException("该手机号已存在对应学员");
        }
        Student student = new Student();
        student.setName(lead.getName());
        student.setPhone(lead.getPhone());
        student.setIdCard(idCard);
        student.setStatus("已报名");
        lead.setStatus("已转学员");
        leadRepository.save(lead);
        return studentRepository.save(student);
    }

    public void delete(Long id) {
        leadRepository.delete(get(id));
    }
}
