package com.example.demo.repository;

import com.example.demo.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    @Query("select s.status, count(s) from Student s group by s.status")
    List<Object[]> countGroupByStatus();

    boolean existsByPhone(String phone);

    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByPhoneOrIdCard(String phone, String idCard);
}
