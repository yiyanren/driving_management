package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.model.Student;
import com.example.demo.model.User;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, StudentRepository studentRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void initUsers() {
        createIfMissing("admin", "admin123", "管理员", "管理员");
        createIfMissing("sales", "sales123", "招生", "招生老师");
        createIfMissing("coach", "coach123", "教练", "教练");
        createIfMissing("student", "student123", "学员", "学员");
        ensureStudentProfile("student", "测试学员", "19900000001", "TEST-STUDENT-001", "未报名");
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled()) || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), token);
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (studentRepository.findByPhoneOrIdCard(req.getPhone(), req.getIdCard()).isPresent()) {
            throw new IllegalArgumentException("电话或身份证已注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("ROLE_STUDENT");
        user.setDisplayName(req.getName());
        user.setEnabled(true);
        // 自主注册驾校为空
        user.setDrivingSchoolId(null);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUserId(user.getId());
        student.setName(req.getName());
        student.setPhone(req.getPhone());
        student.setIdCard(req.getIdCard());
        student.setStatus("未报名");
        student.setDrivingSchoolId(null);
        student.setLicenseType(null);
        studentRepository.save(student);
    }

    private void createIfMissing(String username, String password, String role, String displayName) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private void ensureStudentProfile(String username, String name, String phone, String idCard, String status) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (studentRepository.findByUserId(user.getId()).isPresent()) {
                return;
            }
            Student student = new Student();
            student.setUserId(user.getId());
            student.setName(name);
            student.setPhone(phone);
            student.setIdCard(idCard);
            student.setStatus(status);
            student.setDrivingSchoolId(user.getDrivingSchoolId());
            student.setLicenseType(null);
            studentRepository.save(student);
        });
    }
}
