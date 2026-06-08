package cl.dssm.dau.dto;

import cl.dssm.dau.model.Role;

public record UserUpdateRequest(
        String fullName,
        String email,
        String providerName,
        Role role,
        Boolean enabled
) {}
