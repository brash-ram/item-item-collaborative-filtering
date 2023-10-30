package com.brash.dto;

import jakarta.validation.constraints.NotNull;

public record UserDTO(
        @NotNull Long id
) {
}
