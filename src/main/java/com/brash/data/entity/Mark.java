package com.brash.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import java.util.Objects;

/**
 * Оценка пользователем элемента системы совместной фильтрации
 */
@Entity
@Table(name = "mark")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Mark implements Comparable<Mark> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Mark_SEQ")
    @SequenceGenerator(name = "Mark_SEQ")
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "mark_value")
    private Double mark;

    @Column(name = "is_generated", nullable = false)
    private boolean isGenerated = false;

    @Override
    public int compareTo(Mark o) {
        return Long.compare(id, o.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Mark mark = (Mark) o;
        return id != null && Objects.equals(id, mark.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}