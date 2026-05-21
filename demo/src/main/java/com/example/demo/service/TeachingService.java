package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.CoursePlan;
import com.example.demo.model.Student;
import com.example.demo.model.TrainingRecord;
import com.example.demo.model.User;
import com.example.demo.repository.CoursePlanRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TrainingRecordRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TeachingService {
    private final CoursePlanRepository coursePlanRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public TeachingService(CoursePlanRepository coursePlanRepository,
                           TrainingRecordRepository trainingRecordRepository,
                           StudentRepository studentRepository,
                           UserRepository userRepository) {
        this.coursePlanRepository = coursePlanRepository;
        this.trainingRecordRepository = trainingRecordRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public PageResult<CoursePlan> pagePlans(int page, int size, Long studentId, String coachName) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<CoursePlan> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (studentId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("studentId"), studentId));
            }
            if (StringUtils.hasText(coachName)) {
                // MySQL 中匹配教练名称或带有时间段后缀的教练名称
                predicate = cb.and(predicate, cb.like(root.get("coachName"), coachName + "%"));
            }
            return predicate;
        };
        return PageResult.from(coursePlanRepository.findAll(spec, pageable));
    }

    public CoursePlan createPlan(CoursePlan plan, String username) {
        Student student = getStudent(plan.getStudentId());
        User coach = resolveCoach(plan.getCoachName());
        validateTeachingPair(username, student, coach, "学员预约培训时只能选择同驾校的教练");
        return coursePlanRepository.save(plan);
    }

    public CoursePlan updatePlanStatus(Long id, String status) {
        CoursePlan plan = coursePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("课程计划不存在"));
        plan.setStatus(status);
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

    public TrainingRecord createRecord(TrainingRecord record, String username) {
        Student student = getStudent(record.getStudentId());
        User coach = resolveCoach(record.getCoachName());
        validateTeachingPair(username, student, coach, "教练创建训练记录时只能选择同驾校的学员");
        return trainingRecordRepository.save(record);
    }

    public Map<String, Object> progress(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        getStudent(studentId);
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

    private Student getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("学员不存在"));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private User resolveCoach(String rawCoachName) {
        String coachName = extractCoachName(rawCoachName);
        User coach = userRepository.findByUsername(coachName)
                .filter(this::isCoachRole)
                .orElse(null);
        if (coach != null) {
            return coach;
        }

        Pageable pageable = PageRequest.of(0, 1);
        Specification<User> spec = (root, query, cb) -> cb.and(
                cb.or(
                        cb.equal(root.get("role"), "教练"),
                        cb.equal(root.get("role"), "ROLE_COACH")
                ),
                cb.equal(root.get("displayName"), coachName)
        );
        return userRepository.findAll(spec, pageable)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("所选教练不存在"));
    }

    private String extractCoachName(String rawCoachName) {
        if (!StringUtils.hasText(rawCoachName)) {
            throw new IllegalArgumentException("请选择教练");
        }
        int slotStart = rawCoachName.indexOf(" (");
        String coachName = slotStart >= 0 ? rawCoachName.substring(0, slotStart) : rawCoachName;
        return coachName.trim();
    }

    private void validateTeachingPair(String username, Student student, User coach, String schoolMismatchMessage) {
        Long studentDrivingSchoolId = student.getDrivingSchoolId();
        Long coachDrivingSchoolId = coach.getDrivingSchoolId();
        if (studentDrivingSchoolId == null) {
            throw new IllegalArgumentException("该学员尚未绑定驾校，无法创建培训计划");
        }
        if (coachDrivingSchoolId == null) {
            throw new IllegalArgumentException("该教练尚未绑定驾校，无法创建培训计划");
        }
        if (!studentDrivingSchoolId.equals(coachDrivingSchoolId)) {
            throw new IllegalArgumentException(schoolMismatchMessage);
        }

        User currentUser = getUserByUsername(username);
        if (isStudentRole(currentUser)) {
            if (student.getUserId() == null || !student.getUserId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("学员只能为自己预约培训");
            }
        }
        if (isCoachRole(currentUser)) {
            if (currentUser.getDrivingSchoolId() == null) {
                throw new IllegalArgumentException("当前教练未绑定驾校，无法创建培训计划");
            }
            if (!currentUser.getDrivingSchoolId().equals(studentDrivingSchoolId)) {
                throw new IllegalArgumentException("教练创建计划时只能选择同驾校的学员");
            }
        }
    }

    private boolean isStudentRole(User user) {
        return "学员".equals(user.getRole()) || "ROLE_STUDENT".equals(user.getRole());
    }

    private boolean isCoachRole(User user) {
        return "教练".equals(user.getRole()) || "ROLE_COACH".equals(user.getRole());
    }
}
