package com.brash.dto;

import jakarta.validation.constraints.NotNull;

public record ItemDTO(
        @NotNull Long id
) {
}
