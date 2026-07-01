package com.studentlife.studentlifejava.repository;

import com.studentlife.studentlifejava.entity.RefreshToken;
import com.studentlife.studentlifejava.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllByUser(@Param("user") Users user);

    void deleteByUser(Users users);

    void deleteByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiredAt < :expiry")
    int deleteAllByExpiredAtBefore(@Param("expiry") Instant expiry);
}
