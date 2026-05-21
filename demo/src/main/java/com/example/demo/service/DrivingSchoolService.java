package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.DrivingSchool;
import com.example.demo.repository.DrivingSchoolRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DrivingSchoolService {
    private final DrivingSchoolRepository drivingSchoolRepository;

    public DrivingSchoolService(DrivingSchoolRepository drivingSchoolRepository) {
        this.drivingSchoolRepository = drivingSchoolRepository;
    }

    public PageResult<DrivingSchool> page(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<DrivingSchool> spec = (root, query, cb) -> {
            if (StringUtils.hasText(keyword)) {
                return cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("phone"), "%" + keyword + "%"),
                        cb.like(root.get("address"), "%" + keyword + "%")
                );
            }
            return cb.conjunction();
        };
        return PageResult.from(drivingSchoolRepository.findAll(spec, pageable));
    }
    
    public List<DrivingSchool> listAll() {
        return drivingSchoolRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public DrivingSchool get(Long id) {
        return drivingSchoolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("驾校不存在"));
    }

    public DrivingSchool create(DrivingSchool drivingSchool) {
        return drivingSchoolRepository.save(drivingSchool);
    }

    public DrivingSchool update(Long id, DrivingSchool payload) {
        DrivingSchool drivingSchool = get(id);
        drivingSchool.setName(payload.getName());
        drivingSchool.setAddress(payload.getAddress());
        drivingSchool.setPhone(payload.getPhone());
        return drivingSchoolRepository.save(drivingSchool);
    }

    public void delete(Long id) {
        drivingSchoolRepository.delete(get(id));
    }
}