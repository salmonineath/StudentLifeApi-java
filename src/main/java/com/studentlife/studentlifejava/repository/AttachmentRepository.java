package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Attachment;
import com.studentlife.studentlifejava.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTask(Task task);

    List<Attachment> findByTaskIn(List<Task> tasks);
}
