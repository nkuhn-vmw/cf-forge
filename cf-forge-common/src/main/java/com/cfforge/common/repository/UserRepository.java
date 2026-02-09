package com.cfforge.common.repository;

import com.cfforge.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUaaUserId(String uaaUserId);
    Optional<User> findByEmail(String email);
}
