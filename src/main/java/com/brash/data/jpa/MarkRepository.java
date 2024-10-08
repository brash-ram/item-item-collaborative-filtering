package com.brash.data.jpa;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {

    Page<Mark> findAllByIsGeneratedAndUser(boolean isGenerated, User user, Pageable pageable);

    Page<Mark> findAllByUser(User user, Pageable pageable);

    List<Mark> findAllByMarkLessThan(double value);

    int countAllByIsGenerated(boolean isGenerated);

    Optional<Mark> findByUserEqualsAndItemEquals(User user, Item item);

//    @Query("SELECT m from Mark m where m.isGenerated = true and m.mark >= max(m.mark)")
    @Query(
            value = "SELECT * from mark where is_generated = true and " +
                    "mark_value > (SELECT avg(mark_value) from mark)",
    nativeQuery = true)
    List<Mark> findAllByUserAndMarkGreaterThenAverage(User user);
}