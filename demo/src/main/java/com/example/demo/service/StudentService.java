package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.DrivingSchool;
import com.example.demo.model.Student;
import com.example.demo.model.User;
import com.example.demo.repository.DrivingSchoolRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
public class StudentService {
    private static final Set<String> SUPPORTED_LICENSE_TYPES = Set.of("A1", "A2", "A3", "B1", "B2", "C1", "C2", "C5", "D", "E", "F", "M", "N", "P");

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DrivingSchoolRepository drivingSchoolRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository, DrivingSchoolRepository drivingSchoolRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.drivingSchoolRepository = drivingSchoolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<Student> page(int page, int size, String keyword, String status, Long drivingSchoolId) {
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
            if (drivingSchoolId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("drivingSchoolId"), drivingSchoolId));
            }
            return predicate;
        };
        return PageResult.from(studentRepository.findAll(spec, pageable));
    }

    public PageResult<Student> pageEnrollmentRecords(int page, int size, String keyword, Long drivingSchoolId, String username) {
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Long effectiveDrivingSchoolId = drivingSchoolId;
        if ("教练".equals(currentUser.getRole())) {
            if (currentUser.getDrivingSchoolId() == null) {
                Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
                return PageResult.from(studentRepository.findAll((root, query, cb) -> cb.disjunction(), pageable));
            }
            effectiveDrivingSchoolId = currentUser.getDrivingSchoolId();
        }

        return page(page, size, keyword, "已报名", effectiveDrivingSchoolId);
    }

    public Student get(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("学员不存在"));
    }
    
    public Student getByUserId(Long userId) {
        return studentRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("学员不存在"));
    }

    public Student getByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return studentRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // 如果通过 userId 找不到（比如早期的演示账号），则尝试通过名字匹配第一条
                    PageResult<Student> res = page(0, 1, user.getDisplayName(), null, null);
                    if (res.content().isEmpty()) {
                        return createProfileForStudentUser(user);
                    }
                    Student student = res.content().get(0);
                    if (student.getUserId() == null) {
                        student.setUserId(user.getId());
                        if (student.getDrivingSchoolId() == null) {
                            student.setDrivingSchoolId(user.getDrivingSchoolId());
                        }
                        if (!StringUtils.hasText(student.getStatus())) {
                            student.setStatus(resolveInitialStatus(student.getDrivingSchoolId()));
                        }
                        student = studentRepository.save(student);
                    }
                    return student;
                });
    }

    public Student create(Student student) {
        if (!StringUtils.hasText(student.getStatus())) {
            student.setStatus("已报名");
        }
        student.setLicenseType(normalizeLicenseType(student.getLicenseType(), false));
        applySubjectProgress(student, student);
        
        // Auto-generate account logic
        if (student.getUserId() == null) {
            String pinyinName = convertToPinyin(student.getName());
            String username = pinyinName;
            int counter = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = pinyinName + counter;
                counter++;
            }
            
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRole("ROLE_STUDENT");
            user.setDisplayName(student.getName());
            user.setEnabled(true);
            user.setDrivingSchoolId(student.getDrivingSchoolId());
            user = userRepository.save(user);
            
            student.setUserId(user.getId());
        } else {
            // Bind existing user if provided
            userRepository.findById(student.getUserId()).ifPresent(user -> {
                user.setDrivingSchoolId(student.getDrivingSchoolId());
                userRepository.save(user);
            });
        }
        
        return studentRepository.save(student);
    }

    public Student update(Long id, Student payload) {
        Student student = get(id);
        student.setName(payload.getName());
        student.setPhone(payload.getPhone());
        student.setIdCard(payload.getIdCard());
        
        // 当从个人中心更新时，可能不会传递 status 和 drivingSchoolId，所以需要判断非空
        if (StringUtils.hasText(payload.getStatus())) {
            student.setStatus(payload.getStatus());
        }
        if (payload.getDrivingSchoolId() != null) {
            student.setDrivingSchoolId(payload.getDrivingSchoolId());
        }
        if (payload.getLicenseType() != null) {
            student.setLicenseType(normalizeLicenseType(payload.getLicenseType(), false));
        }
        applySubjectProgress(student, payload);
        
        if (student.getUserId() != null) {
             userRepository.findById(student.getUserId()).ifPresent(user -> {
                if (StringUtils.hasText(payload.getName())) {
                    user.setDisplayName(payload.getName());
                }
                if (payload.getDrivingSchoolId() != null) {
                    user.setDrivingSchoolId(payload.getDrivingSchoolId());
                }
                userRepository.save(user);
            });
        }
        
        return studentRepository.save(student);
    }

    public Student updateStatus(Long id, String status) {
        Student student = get(id);
        student.setStatus(status);
        return studentRepository.save(student);
    }

    public Student enroll(String username, Long drivingSchoolId, String licenseType) {
        if (drivingSchoolId == null) {
            throw new IllegalArgumentException("请选择报名驾校");
        }
        String normalizedLicenseType = normalizeLicenseType(licenseType, true);
        DrivingSchool drivingSchool = drivingSchoolRepository.findById(drivingSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException("驾校不存在"));
        Student student = getByUsername(username);
        if ("已报名".equals(student.getStatus())
                && student.getDrivingSchoolId() != null
                && StringUtils.hasText(student.getLicenseType())) {
            throw new IllegalArgumentException("您已完成报名，无需重复提交");
        }

        student.setStatus("已报名");
        student.setDrivingSchoolId(drivingSchool.getId());
        student.setLicenseType(normalizedLicenseType);
        Student savedStudent = studentRepository.save(student);

        if (savedStudent.getUserId() != null) {
            userRepository.findById(savedStudent.getUserId()).ifPresent(user -> {
                user.setDrivingSchoolId(drivingSchool.getId());
                userRepository.save(user);
            });
        }
        return savedStudent;
    }

    public void delete(Long id) {
        studentRepository.delete(get(id));
    }
    
    private String convertToPinyin(String name) {
        if (!StringUtils.hasText(name)) {
            return "student";
        }
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        
        StringBuilder sb = new StringBuilder();
        try {
            for (char c : name.toCharArray()) {
                if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        sb.append(pinyinArray[0]);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString().replaceAll("[^a-zA-Z0-9]", "");
        } catch (Exception e) {
            return "student";
        }
    }

    private Student createProfileForStudentUser(User user) {
        Student student = new Student();
        student.setUserId(user.getId());
        student.setName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername());
        student.setPhone(generatePlaceholderPhone(user.getId()));
        student.setIdCard("AUTO-" + user.getId());
        student.setStatus(resolveInitialStatus(user.getDrivingSchoolId()));
        initializeSubjectProgress(student);
        student.setDrivingSchoolId(user.getDrivingSchoolId());
        return studentRepository.save(student);
    }

    private String resolveInitialStatus(Long drivingSchoolId) {
        return drivingSchoolId == null ? "未报名" : "已报名";
    }

    private String normalizeLicenseType(String licenseType, boolean required) {
        if (!StringUtils.hasText(licenseType)) {
            if (required) {
                throw new IllegalArgumentException("请选择报名类型");
            }
            return null;
        }
        String normalized = licenseType.trim().toUpperCase();
        if (!SUPPORTED_LICENSE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("报名类型不支持，请重新选择");
        }
        return normalized;
    }

    private String generatePlaceholderPhone(Long userId) {
        long seed = userId == null ? 0L : userId;
        String suffix = String.format("%08d", seed % 100000000L);
        String phone = "199" + suffix;
        while (studentRepository.existsByPhone(phone)) {
            seed++;
            suffix = String.format("%08d", seed % 100000000L);
            phone = "199" + suffix;
        }
        return phone;
    }

    private void applySubjectProgress(Student target, Student source) {
        boolean subjectOnePassed = readBoolean(source.getSubjectOnePassed(), target.getSubjectOnePassed());
        boolean subjectTwoPassed = readBoolean(source.getSubjectTwoPassed(), target.getSubjectTwoPassed());
        boolean subjectThreePassed = readBoolean(source.getSubjectThreePassed(), target.getSubjectThreePassed());
        boolean subjectFourPassed = readBoolean(source.getSubjectFourPassed(), target.getSubjectFourPassed());

        validateSubjectProgress(subjectOnePassed, subjectTwoPassed, subjectThreePassed, subjectFourPassed);

        target.setSubjectOnePassed(subjectOnePassed);
        target.setSubjectTwoPassed(subjectTwoPassed);
        target.setSubjectThreePassed(subjectThreePassed);
        target.setSubjectFourPassed(subjectFourPassed);
    }

    private boolean readBoolean(Boolean incomingValue, Boolean currentValue) {
        if (incomingValue != null) {
            return incomingValue;
        }
        return Boolean.TRUE.equals(currentValue);
    }

    private void initializeSubjectProgress(Student student) {
        student.setSubjectOnePassed(false);
        student.setSubjectTwoPassed(false);
        student.setSubjectThreePassed(false);
        student.setSubjectFourPassed(false);
    }

    private void validateSubjectProgress(boolean subjectOnePassed,
                                         boolean subjectTwoPassed,
                                         boolean subjectThreePassed,
                                         boolean subjectFourPassed) {
        if (subjectFourPassed && !subjectThreePassed) {
            throw new IllegalArgumentException("勾选科目四前必须先勾选科目三");
        }
        if (subjectThreePassed && !subjectTwoPassed) {
            throw new IllegalArgumentException("勾选科目三前必须先勾选科目二");
        }
        if (subjectTwoPassed && !subjectOnePassed) {
            throw new IllegalArgumentException("勾选科目二前必须先勾选科目一");
        }
    }
}
