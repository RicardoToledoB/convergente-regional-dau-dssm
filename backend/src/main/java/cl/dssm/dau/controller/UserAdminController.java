package cl.dssm.dau.controller;

import cl.dssm.dau.dto.*;
import cl.dssm.dau.entity.UserAccount;
import cl.dssm.dau.repository.UserAccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<Page<UserResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"));
        return new ApiResponse<>(true, "Usuarios", users.findAll(pageable).map(this::toResponse));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese username");
        }
        UserAccount u = new UserAccount();
        u.setUsername(request.username());
        u.setPassword(passwordEncoder.encode(request.password()));
        u.setFullName(request.fullName());
        u.setEmail(request.email());
        u.setProviderName(request.providerName());
        u.setRole(request.role());
        u.setEnabled(request.enabled() == null || request.enabled());
        return new ApiResponse<>(true, "Usuario creado", toResponse(users.save(u)));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        UserAccount u = users.findById(id).orElseThrow();
        if (request.fullName() != null) u.setFullName(request.fullName());
        if (request.email() != null) u.setEmail(request.email());
        if (request.providerName() != null) u.setProviderName(request.providerName());
        if (request.role() != null) u.setRole(request.role());
        if (request.enabled() != null) u.setEnabled(request.enabled());
        return new ApiResponse<>(true, "Usuario actualizado", toResponse(users.save(u)));
    }

    @PatchMapping("/{id}/password")
    public ApiResponse<UserResponse> changePassword(@PathVariable Long id, @RequestBody @Valid PasswordUpdateRequest request) {
        UserAccount u = users.findById(id).orElseThrow();
        u.setPassword(passwordEncoder.encode(request.password()));
        return new ApiResponse<>(true, "Clave actualizada", toResponse(users.save(u)));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<UserResponse> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        UserAccount u = users.findById(id).orElseThrow();
        u.setEnabled(enabled);
        return new ApiResponse<>(true, enabled ? "Usuario habilitado" : "Usuario deshabilitado", toResponse(users.save(u)));
    }

    private UserResponse toResponse(UserAccount u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getProviderName(), u.getRole(), u.getEnabled(), u.getCreatedAt(), u.getUpdatedAt(), u.getLastLogin());
    }
}
