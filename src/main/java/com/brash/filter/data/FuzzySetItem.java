package com.brash.filter.data;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FuzzySetItem(
        Mark mark,

        @Min(0)
        @Max(1)
        int preference
) {}
