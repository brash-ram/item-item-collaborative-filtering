package com.brash.filter.data;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class FuzzySet {
    List<FuzzySetItem> set;
    UserPreferenceOnFuzzySet preferenceOnFuzzySet;
    UserPreferenceOnVagueSet preferenceOnVagueSet;
}
