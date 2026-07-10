package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.ChecklistItem;
import com.studentlife.studentlifejava.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByTaskOrderByIdAsc(Task task);

    List<ChecklistItem> findByTaskInOrderByIdAsc(List<Task> tasks);
}
