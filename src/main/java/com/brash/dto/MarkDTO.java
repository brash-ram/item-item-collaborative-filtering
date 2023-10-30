package com.brash.dto;

import jakarta.validation.constraints.NotNull;

public record MarkDTO (
        @NotNull Long userId,
        @NotNull Long itemId,
        @NotNull Double mark
        ) {
}
