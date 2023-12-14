package com.brash.data.entity;

import com.brash.util.Utils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import java.util.*;

/**
 * Элемент системы совместной фильтрации
 */
@Entity
@Table(name = "item")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Item implements Comparable<Item>, HavingMarks {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Item_SEQ")
    @SequenceGenerator(name = "Item_SEQ")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "original_id", nullable = false)
    private Long originalId;


    @OneToMany(fetch = FetchType.EAGER, mappedBy = "item")
    @ToString.Exclude
    private SortedSet<Mark> marks = new TreeSet<>();

    @Transient
    private volatile List<Mark> notGeneratedMarks;

    @Transient
    private volatile double averageMark = 0.0;

    @Transient
    private final Object lock = new Object();

    public double getAverageMarks() {
        if (averageMark == 0.0 && getNotGeneratedMarks().size() != 0) {
            synchronized (lock) {
                if (averageMark == 0.0) {
                    averageMark = Utils.getAverageMark(notGeneratedMarks);
                }
            }

        }
        return averageMark;
    }

    public List<Mark> getNotGeneratedMarks() {
        if (notGeneratedMarks == null) {
            synchronized (lock) {
                if (notGeneratedMarks == null) {
                    notGeneratedMarks = marks.stream()
                            .filter(mark -> !mark.getIsGenerated())
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
        Item item = (Item) o;
        return id != null && Objects.equals(id, item.id);
    }

    @Override
    public SortedSet<Mark> getMarks() {
        return marks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Item o) {
        return Long.compare(id, o.getId());
    }
}