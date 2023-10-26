package com.brash.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Table(name = "item")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Item_SEQ")
    @SequenceGenerator(name = "Item_SEQ")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "original_id", nullable = false)
    private Long originalId;

}