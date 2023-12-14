package com.brash.dto.web;

import jakarta.validation.constraints.NotNull;

public record UserDTO(
        @NotNull Long id
) {
}
