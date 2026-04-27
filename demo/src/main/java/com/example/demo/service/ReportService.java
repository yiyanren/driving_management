package com.example.demo.service;

import com.example.demo.repository.ExamApplicationRepository;
import com.example.demo.repository.LeadRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TrainingRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportService {
    private final LeadRepository leadRepository;
    private final StudentRepository studentRepository;
    private final ExamApplicationRepository examApplicationRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    public ReportService(LeadRepository leadRepository,
                         StudentRepository studentRepository,
                         ExamApplicationRepository examApplicationRepository,
                         TrainingRecordRepository trainingRecordRepository) {
        this.leadRepository = leadRepository;
        this.studentRepository = studentRepository;
        this.examApplicationRepository = examApplicationRepository;
        this.trainingRecordRepository = trainingRecordRepository;
    }

    public Map<String, Object> overview(LocalDate from, LocalDate to) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("leadTotal", leadRepository.count());
        result.put("studentTotal", studentRepository.count());
        result.put("examApplicationTotal", examApplicationRepository.count());
        result.put("leadStatusStats", toMap(leadRepository.countGroupByStatus()));
        result.put("leadSourceStats", toMap(leadRepository.countGroupBySource()));
        result.put("ownerPerformance", toMap(leadRepository.conversionByOwner()));
        result.put("studentStatusStats", toMap(studentRepository.countGroupByStatus()));
        result.put("examStatusStats", toMap(examApplicationRepository.countGroupByStatus()));
        result.put("trainingHoursBySubject", toMap(trainingRecordRepository.sumHoursGroupBySubjectCode()));
        return result;
    }

    public Map<String, Object> funnel(LocalDate from, LocalDate to) {
        long leadTotal = leadRepository.count();
        long studentTotal = studentRepository.count();
        long applicationTotal = examApplicationRepository.count();
        long approvedTotal = extractCount(examApplicationRepository.countGroupByStatus(), "已通过");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("leadTotal", leadTotal);
        result.put("studentTotal", studentTotal);
        result.put("applicationTotal", applicationTotal);
        result.put("approvedTotal", approvedTotal);
        result.put("leadToStudentRate", rate(studentTotal, leadTotal));
        result.put("studentToApplyRate", rate(applicationTotal, studentTotal));
        result.put("applyToApprovedRate", rate(approvedTotal, applicationTotal));
        return result;
    }

    public String overviewCsv(LocalDate from, LocalDate to) {
        Map<String, Object> overview = overview(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("key,value\n");
        appendCsvRow(sb, "from", overview.get("from"));
        appendCsvRow(sb, "to", overview.get("to"));
        appendCsvRow(sb, "leadTotal", overview.get("leadTotal"));
        appendCsvRow(sb, "studentTotal", overview.get("studentTotal"));
        appendCsvRow(sb, "examApplicationTotal", overview.get("examApplicationTotal"));
        appendCsvRow(sb, "leadSourceStats", overview.get("leadSourceStats"));
        appendCsvRow(sb, "ownerPerformance", overview.get("ownerPerformance"));
        appendCsvRow(sb, "examStatusStats", overview.get("examStatusStats"));
        return sb.toString();
    }

    private Map<String, Object> toMap(List<Object[]> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), row[1]);
        }
        return result;
    }

    private long extractCount(List<Object[]> rows, String key) {
        return rows.stream()
                .filter(row -> Objects.equals(String.valueOf(row[0]), key))
                .map(row -> (Number) row[1])
                .mapToLong(Number::longValue)
                .findFirst()
                .orElse(0L);
    }

    private String rate(long numerator, long denominator) {
        if (denominator == 0) {
            return "0.00%";
        }
        double val = (numerator * 100.0) / denominator;
        return String.format("%.2f%%", val);
    }

    private void appendCsvRow(StringBuilder sb, String key, Object value) {
        String val = value == null ? "" : String.valueOf(value).replace("\"", "\"\"");
        sb.append(key).append(",\"").append(val).append("\"\n");
    }
}
