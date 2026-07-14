package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Assignment;
import com.studentlife.studentlifejava.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignmentOrderBySortOrderAsc(Assignment assignment);

    long countByAssignment(Assignment assignment);

    // Deletions can leave gaps in sortOrder, so the next slot must be derived from
    // the highest existing value rather than a row count - otherwise a new task can
    // collide with a surviving task's sortOrder. See TaskServiceImpl#create.
    @Query("SELECT COALESCE(MAX(t.sortOrder), -1) FROM Task t WHERE t.assignment = :assignment")
    int findMaxSortOrder(@Param("assignment") Assignment assignment);
}
