package com.brash.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

@Entity
@Table(name = "item")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Item implements Comparable<Item> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Item_SEQ")
    @SequenceGenerator(name = "Item_SEQ")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "original_id", nullable = false)
    private Long originalId;


    @OneToMany(mappedBy = "item")
    @ToString.Exclude
    private SortedSet<Mark> marks = new TreeSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Item item = (Item) o;
        return id != null && Objects.equals(id, item.id);
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