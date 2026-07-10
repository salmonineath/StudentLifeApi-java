package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignmentOrderBySortOrderAsc(Assignment assignment);

    long countByAssignment(Assignment assignment);
}
