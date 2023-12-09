package com.brash.filter.data;

import com.brash.data.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Пара схожих элементов и их оценка схожести
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@Data
public class SimilarItems {

     public FuzzySet fuzzySet1;

     public FuzzySet fuzzySet2;

     /**
      * Значение сходства элементов
      */
     public double similarValue;

     public double divergenceKL;
}
