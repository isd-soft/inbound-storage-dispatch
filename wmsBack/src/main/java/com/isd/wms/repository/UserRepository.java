package com.isd.wms.repository;

import com.isd.wms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    List<User> findAllByIsActiveTrue();

    List<User> findAllByEmailVerifiedFalseAndVerificationTokenExpiresAtBefore(LocalDateTime now);
}
