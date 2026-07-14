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

    // Conditional UPDATE used as an atomic compare-and-set: two concurrent refresh
    // requests presenting the same token can both read revoked=false, but only one
    // can win this update (returns 1). The loser sees 0 and is treated as reuse.
    // Read-then-save on the entity would have a TOCTOU gap; this doesn't.
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.id = :id AND rt.revoked = false")
    int revokeIfActive(@Param("id") Long id);

    // Also purges revoked rows regardless of expiry - otherwise every rotated or
    // reuse-detected token sits in the table until its original expiry date, even
    // though it can never be used again. Never touches a still-active token.
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiredAt < :expiry OR rt.revoked = true")
    int deleteAllByExpiredAtBefore(@Param("expiry") Instant expiry);
}
