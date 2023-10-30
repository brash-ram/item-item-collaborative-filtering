package com.brash.dto;

import jakarta.validation.constraints.NotNull;

public record AddMarkDTO(
        @NotNull Long userId,
        @NotNull Long itemId,
        @NotNull Integer mark
) {
}
