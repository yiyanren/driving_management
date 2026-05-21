package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.dto.ExamSiteRealtimeDto;
import com.example.demo.dto.ExamSiteScheduleRequest;
import com.example.demo.dto.ExamSiteTrendPointDto;
import com.example.demo.model.ExamSite;
import com.example.demo.model.ExamSiteCapacitySnapshot;
import com.example.demo.model.ExamSiteSchedule;
import com.example.demo.repository.ExamSiteCapacitySnapshotRepository;
import com.example.demo.repository.ExamSiteRepository;
import com.example.demo.repository.ExamSiteScheduleRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExamSiteService {
    private static final Pattern REGION_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]+区)");

    private final ExamSiteRepository examSiteRepository;
    private final ExamSiteScheduleRepository examSiteScheduleRepository;
    private final ExamSiteCapacitySnapshotRepository examSiteCapacitySnapshotRepository;
    private final String localExamSiteFilePath;

    public ExamSiteService(ExamSiteRepository examSiteRepository,
                           ExamSiteScheduleRepository examSiteScheduleRepository,
                           ExamSiteCapacitySnapshotRepository examSiteCapacitySnapshotRepository,
                           @Value("${app.exam-site.local-file-path:../考场信息表.xlsx}") String localExamSiteFilePath) {
        this.examSiteRepository = examSiteRepository;
        this.examSiteScheduleRepository = examSiteScheduleRepository;
        this.examSiteCapacitySnapshotRepository = examSiteCapacitySnapshotRepository;
        this.localExamSiteFilePath = localExamSiteFilePath;
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

    public Map<String, Object> importExcel(MultipartFile file, Integer defaultCapacity) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传考场信息表");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return doImportExcel(inputStream, defaultCapacity);
        }
    }

    public Map<String, Object> importLocalExcel(Integer defaultCapacity) throws IOException {
        try (InputStream inputStream = new FileInputStream(localExamSiteFilePath)) {
            return doImportExcel(inputStream, defaultCapacity);
        }
    }

    private Map<String, Object> doImportExcel(InputStream inputStream, Integer defaultCapacity) throws IOException {
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
        result.put("sourcePath", localExamSiteFilePath);
        return result;
    }

    public ExamSiteSchedule saveSchedule(ExamSiteScheduleRequest request) {
        get(request.getExamSiteId());
        int total = Math.max(request.getTotalCapacity(), 0);
        int reserved = Math.max(Math.min(request.getReservedCount(), total), 0);
        ExamSiteSchedule schedule = examSiteScheduleRepository
                .findByExamSiteIdAndExamDateAndSubjectCode(request.getExamSiteId(), request.getExamDate(), request.getSubjectCode())
                .orElseGet(ExamSiteSchedule::new);
        schedule.setExamSiteId(request.getExamSiteId());
        schedule.setExamDate(request.getExamDate());
        schedule.setSubjectCode(request.getSubjectCode());
        schedule.setTotalCapacity(total);
        schedule.setReservedCount(reserved);
        schedule.setRemainingCount(total - reserved);
        ExamSiteSchedule saved = examSiteScheduleRepository.save(schedule);
        saveSnapshot(saved);
        return saved;
    }

    public PageResult<ExamSiteRealtimeDto> realtimePage(int page,
                                                        int size,
                                                        LocalDate examDate,
                                                        String subjectCode,
                                                        String regionName,
                                                        String keyword,
                                                        Boolean onlyAvailable) {
        Specification<ExamSiteSchedule> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (examDate != null) {
                predicate = cb.and(predicate, cb.equal(root.get("examDate"), examDate));
            }
            if (StringUtils.hasText(subjectCode)) {
                predicate = cb.and(predicate, cb.equal(root.get("subjectCode"), subjectCode));
            }
            if (Boolean.TRUE.equals(onlyAvailable)) {
                predicate = cb.and(predicate, cb.greaterThan(root.get("remainingCount"), 0));
            }
            return predicate;
        };
        List<ExamSiteSchedule> schedules = examSiteScheduleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "syncTime"));
        List<Long> siteIds = schedules.stream().map(ExamSiteSchedule::getExamSiteId).distinct().toList();
        Map<Long, ExamSite> siteMap = examSiteRepository.findByIdIn(siteIds).stream()
                .collect(Collectors.toMap(ExamSite::getId, x -> x));

        List<ExamSiteRealtimeDto> rows = new ArrayList<>();
        for (ExamSiteSchedule schedule : schedules) {
            ExamSite site = siteMap.get(schedule.getExamSiteId());
            if (site == null) {
                continue;
            }
            if (StringUtils.hasText(regionName) && !regionName.equals(site.getRegionName())) {
                continue;
            }
            if (StringUtils.hasText(keyword)
                    && !(contains(site.getName(), keyword)
                    || contains(site.getAddress(), keyword)
                    || contains(site.getVehicleTypes(), keyword))) {
                continue;
            }
            ExamSiteRealtimeDto dto = new ExamSiteRealtimeDto();
            dto.setSiteId(site.getId());
            dto.setSiteName(site.getName());
            dto.setRegionName(site.getRegionName());
            dto.setSubjectType(site.getSubjectType());
            dto.setVehicleTypes(site.getVehicleTypes());
            dto.setExamDate(schedule.getExamDate());
            dto.setSubjectCode(schedule.getSubjectCode());
            dto.setTotalCapacity(schedule.getTotalCapacity());
            dto.setReservedCount(schedule.getReservedCount());
            dto.setRemainingCount(schedule.getRemainingCount());
            dto.setSyncTime(schedule.getSyncTime());
            rows.add(dto);
        }
        rows.sort(Comparator.comparing(ExamSiteRealtimeDto::getSyncTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, rows.size());
        int toIndex = Math.min(fromIndex + safeSize, rows.size());
        int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / safeSize);
        return new PageResult<>(rows.subList(fromIndex, toIndex), rows.size(), totalPages, safePage, safeSize);
    }

    public List<ExamSiteTrendPointDto> trend(Long siteId, String subjectCode, LocalDate from, LocalDate to) {
        get(siteId);
        LocalDateTime start = (from == null ? LocalDate.now().minusDays(7) : from).atStartOfDay();
        LocalDateTime end = (to == null ? LocalDate.now() : to).atTime(LocalTime.MAX);
        return examSiteCapacitySnapshotRepository
                .findByExamSiteIdAndSubjectCodeAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(siteId, subjectCode, start, end)
                .stream()
                .map(x -> {
                    ExamSiteTrendPointDto dto = new ExamSiteTrendPointDto();
                    dto.setSnapshotTime(x.getSnapshotTime());
                    dto.setReservedCount(x.getReservedCount());
                    dto.setRemainingCount(x.getRemainingCount());
                    return dto;
                })
                .toList();
    }

    public void consumeScheduleCapacity(Long examSiteId, LocalDate examDate, String subjectCode) {
        examSiteScheduleRepository.findByExamSiteIdAndExamDateAndSubjectCode(examSiteId, examDate, subjectCode)
                .ifPresent(schedule -> {
                    if (schedule.getRemainingCount() <= 0) {
                        throw new IllegalArgumentException("该日期该科目考场余位不足");
                    }
                    schedule.setReservedCount(schedule.getReservedCount() + 1);
                    schedule.setRemainingCount(schedule.getRemainingCount() - 1);
                    ExamSiteSchedule saved = examSiteScheduleRepository.save(schedule);
                    saveSnapshot(saved);
                });
    }

    private void saveSnapshot(ExamSiteSchedule schedule) {
        ExamSiteCapacitySnapshot snapshot = new ExamSiteCapacitySnapshot();
        snapshot.setExamSiteId(schedule.getExamSiteId());
        snapshot.setExamDate(schedule.getExamDate());
        snapshot.setSubjectCode(schedule.getSubjectCode());
        snapshot.setTotalCapacity(schedule.getTotalCapacity());
        snapshot.setReservedCount(schedule.getReservedCount());
        snapshot.setRemainingCount(schedule.getRemainingCount());
        examSiteCapacitySnapshotRepository.save(snapshot);
    }

    private boolean contains(String text, String keyword) {
        return StringUtils.hasText(text) && text.contains(keyword);
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
