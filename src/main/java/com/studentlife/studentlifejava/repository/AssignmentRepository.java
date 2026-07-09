package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Page<Assignment> findByCreatedBy(Users createdBy, Pageable pageable);
}
