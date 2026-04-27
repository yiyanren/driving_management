package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public PageResult<Student> page(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<Student> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(keyword)) {
                var p = cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("phone"), "%" + keyword + "%"),
                        cb.like(root.get("idCard"), "%" + keyword + "%")
                );
                predicate = cb.and(predicate, p);
            }
            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
        return PageResult.from(studentRepository.findAll(spec, pageable));
    }

    public Student get(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("学员不存在"));
    }

    public Student create(Student student) {
        if (!StringUtils.hasText(student.getStatus())) {
            student.setStatus("已报名");
        }
        return studentRepository.save(student);
    }

    public Student update(Long id, Student payload) {
        Student student = get(id);
        student.setName(payload.getName());
        student.setPhone(payload.getPhone());
        student.setIdCard(payload.getIdCard());
        student.setStatus(payload.getStatus());
        return studentRepository.save(student);
    }

    public Student updateStatus(Long id, String status) {
        Student student = get(id);
        student.setStatus(status);
        return studentRepository.save(student);
    }

    public void delete(Long id) {
        studentRepository.delete(get(id));
    }
}
