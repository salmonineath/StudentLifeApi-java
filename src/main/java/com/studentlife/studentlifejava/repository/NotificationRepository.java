package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Notification;
import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserOrderByCreatedAtDesc(Users user, Pageable pageable);

    Optional<Notification> findByIdAndUser(Long id, Users user);

    long countByUserAndIsReadFalse(Users user);

    // Bulk update instead of loading every unread row into memory - a user
    // can accumulate hundreds of notifications and read-all only needs to
    // flip a flag, not round-trip each entity through Hibernate.
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadForUser(@Param("user") Users user);
}
