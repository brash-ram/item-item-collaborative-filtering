package com.brash.data.jpa;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {

    List<Mark> findAllByIsGenerated(boolean isGenerated);

    Optional<Mark> findByUserEqualsAndItemEquals(User user, Item item);
}