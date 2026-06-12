package cl.dssm.dau.service;

import cl.dssm.dau.dto.AuthRequest;
import cl.dssm.dau.dto.AuthResponse;
import cl.dssm.dau.repository.UserAccountRepository;
import cl.dssm.dau.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserAccountRepository users;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var userDetails = userDetailsService.loadUserByUsername(request.username());
        var user = users.findByUsername(request.username()).orElseThrow();
        user.setLastLogin(LocalDateTime.now());
        users.save(user);
        var token = jwtService.generateToken(userDetails, Map.of(
                "role", user.getRole().name(),
                "fullName", user.getFullName(),
                "providerName", user.getProviderName() == null ? "" : user.getProviderName()
        ));
        return new AuthResponse(token, user.getUsername(), user.getFullName(), user.getRole().name(), user.getProviderName());
    }
}
