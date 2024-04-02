package com.brash.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import java.util.List;
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
public class User implements HavingMarks {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "User_SEQ")
    @SequenceGenerator(name = "User_SEQ")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "original_id", nullable = false)
    private Long originalId;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", originalId=" + originalId +
                '}';
    }

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
    @ToString.Exclude
    private SortedSet<Mark> marks = new TreeSet<>();

    @Transient
    private volatile List<Mark> notGeneratedMarks;

    @Transient
    private final Object lock = new Object();

    public List<Mark> getNotGeneratedMarks() {
        if (notGeneratedMarks == null) {
            synchronized (lock) {
                if (notGeneratedMarks == null) {
                    notGeneratedMarks = marks.stream()
                            .filter(mark -> !mark.isGenerated())
                            .toList();
                }
            }

        }
        return notGeneratedMarks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public SortedSet<Mark> getMarks() {
        return marks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}