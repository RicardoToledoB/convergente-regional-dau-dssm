package cl.dssm.dau.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    @Value("${app.integration.api-key-hcm:CAMBIAR_API_KEY_HCM}")
    private String apiKeyHcm;

    @Value("${app.patient.api-key:}")
    private String patientApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.equals("/api/integration/dau/eventos") && !uri.startsWith("/api/patient/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader("X-API-KEY");
        String uri = request.getRequestURI();

        if (uri.equals("/api/integration/dau/eventos")
                && incoming != null
                && incoming.equals(apiKeyHcm)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "HCM_INTEGRATION", null, List.of(new SimpleGrantedAuthority("ROLE_INTEGRATION"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (uri.startsWith("/api/patient/")
                && patientApiKey != null
                && !patientApiKey.isBlank()
                && incoming != null
                && incoming.equals(patientApiKey)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "PATIENT_API", null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT_API"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
