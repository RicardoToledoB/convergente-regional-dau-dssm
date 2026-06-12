package cl.dssm.dau.controller;

import cl.dssm.dau.dto.ApiResponse;
import cl.dssm.dau.dto.AuthRequest;
import cl.dssm.dau.dto.AuthResponse;
import cl.dssm.dau.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return new ApiResponse<>(true, "Autenticado", authService.login(request));
    }
}
