package com.brash.dto.web;

import jakarta.validation.constraints.NotNull;

public record MarkDTO (
        @NotNull Long userId,
        @NotNull Long itemId,
        @NotNull Double mark
        ) {
}
