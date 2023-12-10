package com.brash.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Пользователь системы совместной фильтрации
 */
@Entity
@Table(name = "user_info")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class User implements HavingMarks {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "User_SEQ")
    @SequenceGenerator(name = "User_SEQ")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "original_id", nullable = false)
    private Long originalId;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private SortedSet<Mark> marks = new TreeSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}