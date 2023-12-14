package com.brash.filter.data;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Нечеткое множество
 */
@Data
@Accessors(chain = true)
public class FuzzySet {
    List<FuzzySetItem> set;
    UserPreferenceOnFuzzySet preferenceOnFuzzySet;
    UserPreferenceOnVagueSet preferenceOnVagueSet;
}
