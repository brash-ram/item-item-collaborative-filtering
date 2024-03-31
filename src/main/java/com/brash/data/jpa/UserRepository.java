package com.brash.data.jpa;

import com.brash.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOriginalId(long originalId);
    boolean existsByOriginalId(long originalId);
}