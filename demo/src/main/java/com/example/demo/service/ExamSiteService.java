package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.ExamSite;
import com.example.demo.repository.ExamSiteRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExamSiteService {
    private static final Pattern REGION_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]+区)");

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
                        cb.like(root.get("address"), "%" + keyword + "%"),
                        cb.like(root.get("subjectType"), "%" + keyword + "%"),
                        cb.like(root.get("vehicleTypes"), "%" + keyword + "%"),
                        cb.like(root.get("regionName"), "%" + keyword + "%")
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
        if (!StringUtils.hasText(examSite.getRegionName())) {
            examSite.setRegionName(extractRegion(examSite.getAddress()));
        }
        examSite.setCapacity(0);
        examSite.setReservedCount(0);
        normalizeCapacity(examSite);
        return examSiteRepository.save(examSite);
    }

    public ExamSite update(Long id, ExamSite payload) {
        ExamSite examSite = get(id);
        examSite.setName(payload.getName());
        examSite.setAddress(payload.getAddress());
        examSite.setSubjectType(payload.getSubjectType());
        examSite.setVehicleTypes(payload.getVehicleTypes());
        examSite.setRegionName(StringUtils.hasText(payload.getRegionName()) ? payload.getRegionName() : extractRegion(payload.getAddress()));
        examSite.setCapacity(0);
        normalizeCapacity(examSite);
        return examSiteRepository.save(examSite);
    }

    public void delete(Long id) {
        examSiteRepository.delete(get(id));
    }

    public Map<String, Object> importExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传考场信息表");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return doImportExcel(inputStream);
        }
    }

    private Map<String, Object> doImportExcel(InputStream inputStream) throws IOException {
        int imported = 0;
        int updated = 0;
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = formatter.formatCellValue(row.getCell(1)).trim();
                String subjectType = formatter.formatCellValue(row.getCell(2)).trim();
                String vehicleTypes = formatter.formatCellValue(row.getCell(3)).trim();
                String address = formatter.formatCellValue(row.getCell(4)).trim();
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                ExamSite entity = examSiteRepository
                        .findFirstByNameAndSubjectTypeAndAddress(name, subjectType, address)
                        .orElseGet(ExamSite::new);
                boolean exists = entity.getId() != null;
                entity.setName(name);
                entity.setSubjectType(subjectType);
                entity.setVehicleTypes(vehicleTypes);
                entity.setAddress(address);
                entity.setRegionName(extractRegion(address));
                entity.setCapacity(0);
                if (entity.getReservedCount() == null) {
                    entity.setReservedCount(0);
                }
                examSiteRepository.save(entity);
                if (exists) {
                    updated++;
                } else {
                    imported++;
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("updated", updated);
        result.put("total", imported + updated);
        return result;
    }

    private String extractRegion(String address) {
        if (!StringUtils.hasText(address)) {
            return "未分区";
        }
        Matcher matcher = REGION_PATTERN.matcher(address);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未分区";
    }

    private void normalizeCapacity(ExamSite examSite) {
        if (examSite.getCapacity() == null) {
            examSite.setCapacity(0);
        }
        if (examSite.getReservedCount() == null) {
            examSite.setReservedCount(0);
        }
        if (!StringUtils.hasText(examSite.getRegionName())) {
            examSite.setRegionName(extractRegion(examSite.getAddress()));
        }
    }
}
