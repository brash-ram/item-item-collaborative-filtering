package com.brash.data.jpa;

import com.brash.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByOriginalId(long originalId);
}