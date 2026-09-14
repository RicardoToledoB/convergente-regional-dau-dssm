package cl.dssm.dau.dto;

import cl.dssm.dau.model.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String providerName,
        Role role,
        Boolean enabled,
        String comuna,
        Integer establecimientoCodigo,
        String logoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLogin
) {}
