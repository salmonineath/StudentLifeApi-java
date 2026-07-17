package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<Users> findWithRolesById(Long id);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmailOrUsername(String email, String username);

    List<Users> findByEmailInIgnoreCase(Collection<String> emails, Pageable pageable);

    @Query("SELECT u FROM Users u WHERE LOWER(u.email) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<Users> findByEmailStartingWithIgnoreCase(@Param("prefix") String prefix, Pageable pageable);
}
