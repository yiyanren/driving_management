package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.ExamSite;
import com.example.demo.repository.ExamSiteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExamSiteService {
    private final ExamSiteRepository examSiteRepository;

    public ExamSiteService(ExamSiteRepository examSiteRepository) {
        this.examSiteRepository = examSiteRepository;
    }

    public PageResult<ExamSite> page(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<ExamSite> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(keyword)) {
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("address"), "%" + keyword + "%")
                ));
            }
            return predicate;
        };
        return PageResult.from(examSiteRepository.findAll(spec, pageable));
    }

    public ExamSite get(Long id) {
        return examSiteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("考场不存在"));
    }

    public ExamSite create(ExamSite examSite) {
        normalizeCapacity(examSite);
        return examSiteRepository.save(examSite);
    }

    public ExamSite update(Long id, ExamSite payload) {
        ExamSite examSite = get(id);
        examSite.setName(payload.getName());
        examSite.setAddress(payload.getAddress());
        examSite.setLatitude(payload.getLatitude());
        examSite.setLongitude(payload.getLongitude());
        examSite.setRouteGuide(payload.getRouteGuide());
        examSite.setSessionName(payload.getSessionName());
        examSite.setCapacity(payload.getCapacity());
        examSite.setReservedCount(payload.getReservedCount());
        normalizeCapacity(examSite);
        return examSiteRepository.save(examSite);
    }

    public void delete(Long id) {
        examSiteRepository.delete(get(id));
    }

    private void normalizeCapacity(ExamSite examSite) {
        if (examSite.getCapacity() == null) {
            examSite.setCapacity(0);
        }
        if (examSite.getReservedCount() == null) {
            examSite.setReservedCount(0);
        }
    }
}
