package cl.dssm.dau.config;

import cl.dssm.dau.entity.UserAccount;
import cl.dssm.dau.model.Role;
import cl.dssm.dau.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfMissing("admin", "admin123", "Administrador DSSM", "admin@dssm.cl", "DSSM", Role.ADMIN);
        createUserIfMissing("HCM_Integracion", "Hcm2026*", "Integracion Hospital Clinico Magallanes", "integracion.hcm@dssm.cl", "Hospital Clinico Magallanes", Role.INTEGRADOR);
        createUserIfMissing("RAYEN_Integracion", "Rayen2026*", "Integracion Rayen Salud", "integracion.rayen@dssm.cl", "Rayen Salud", Role.INTEGRADOR);
    }

    private void createUserIfMissing(String username, String rawPassword, String fullName, String email, String providerName, Role role) {
        users.findByUsername(username).orElseGet(() -> {
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setFullName(fullName);
            u.setEmail(email);
            u.setProviderName(providerName);
            u.setRole(role);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setEnabled(true);
            return users.save(u);
        });
    }
}
