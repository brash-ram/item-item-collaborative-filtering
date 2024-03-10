package com.brash.dto.web;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ItemsDTO (
        @NotNull List<Long> ids
) {
}
