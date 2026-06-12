package cl.dssm.dau.dto;

import cl.dssm.dau.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String fullName,
        String email,
        String providerName,
        @NotNull Role role,
        Boolean enabled
) {}
