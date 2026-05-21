package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.common.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<User> page(int page, int size, String role, Long drivingSchoolId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Specification<User> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(role)) {
                predicate = cb.and(predicate, cb.equal(root.get("role"), role));
            }
            if (drivingSchoolId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("drivingSchoolId"), drivingSchoolId));
            }
            return predicate;
        };
        return PageResult.from(userRepository.findAll(spec, pageable));
    }

    public User create(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = getById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User update(Long id, User payload) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        if (StringUtils.hasText(payload.getDisplayName())) {
            user.setDisplayName(payload.getDisplayName());
        }
        if (StringUtils.hasText(payload.getPassword()) && !payload.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(payload.getPassword()));
        }
        if (payload.getEnabled() != null) {
            user.setEnabled(payload.getEnabled());
        }
        if (payload.getDrivingSchoolId() != null) user.setDrivingSchoolId(payload.getDrivingSchoolId());
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
