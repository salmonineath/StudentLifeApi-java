package com.studentlife.studentlifejava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private Instant expiredAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column
    private Instant rotatedAt;

    // Set by Hibernate at persist time, not construction time - matches the
    // pattern Users.createdAt uses, and survives object reconstruction (test
    // fixtures, batch jobs) unlike a plain field initializer would.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
