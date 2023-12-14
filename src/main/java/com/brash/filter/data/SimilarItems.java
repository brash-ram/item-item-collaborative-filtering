package com.brash.filter.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Пара схожих нечетких множеств, в которых определяется сходство элементов или пользователей.
 * В первом случае в каждом элементе нечеткого множества один и тот же элемент, а все пользователи разные.
 * В другом случае соответственно один и тот же пользователь и разные элементы.
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
