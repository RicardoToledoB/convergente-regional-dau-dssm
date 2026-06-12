package cl.dssm.dau.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(@NotBlank String password) {}
