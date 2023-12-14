package com.brash.dto.web;

import jakarta.validation.constraints.NotNull;

public record ItemDTO(
        @NotNull Long id
) {
}
