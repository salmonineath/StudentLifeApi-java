package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmailOrUsername(String email, String username);
}
