package cl.dssm.dau.controller;

import cl.dssm.dau.dto.ApiResponse;
import cl.dssm.dau.dto.PantallaApsResponse;
import cl.dssm.dau.dto.EstablecimientoPantallaResponse;
import cl.dssm.dau.dto.RedUrgenciaResponse;
import cl.dssm.dau.entity.UserAccount;
import cl.dssm.dau.model.Role;
import cl.dssm.dau.repository.UserAccountRepository;
import cl.dssm.dau.service.PantallaApsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pantallas/aps")
@PreAuthorize("hasAnyRole('ADMIN','VISOR_APS','GESTOR_COMUNAL','GESTOR_REGIONAL')")
@RequiredArgsConstructor
public class PantallaApsController {
    private final PantallaApsService service;
    private final UserAccountRepository users;

    @GetMapping
    public ApiResponse<PantallaApsResponse> getPantalla(@RequestParam(required = false) Integer establecimiento,
                                                        Authentication authentication) {
        UserAccount user = currentUser(authentication);
        Integer codigo = establecimiento;
        if (user.getRole() == Role.VISOR_APS && user.getEstablecimientoCodigo() != null) {
            codigo = user.getEstablecimientoCodigo();
        }
        if (user.getRole() == Role.GESTOR_COMUNAL && codigo != null && user.getComuna() != null
                && !user.getComuna().equalsIgnoreCase(service.comunaEstablecimiento(codigo))) {
            throw new IllegalArgumentException("El establecimiento no pertenece a la comuna asociada al usuario");
        }
        return new ApiResponse<>(true, "Pantalla APS generada", service.getPantalla(codigo));
    }

    @GetMapping("/establecimientos")
    public ApiResponse<List<EstablecimientoPantallaResponse>> getEstablecimientos(Authentication authentication) {
        UserAccount user = currentUser(authentication);
        List<EstablecimientoPantallaResponse> rows = service.getEstablecimientos();
        if (user.getRole() == Role.VISOR_APS && user.getEstablecimientoCodigo() != null) {
            rows = rows.stream().filter(e -> user.getEstablecimientoCodigo().equals(e.codigo())).toList();
        } else if (user.getRole() == Role.GESTOR_COMUNAL && user.getComuna() != null && !user.getComuna().isBlank()) {
            rows = rows.stream().filter(e -> user.getComuna().equalsIgnoreCase(service.comunaEstablecimiento(e.codigo()))).toList();
        }
        return new ApiResponse<>(true, "Establecimientos disponibles", rows);
    }

    @GetMapping("/comunas")
    public ApiResponse<List<String>> getComunas(Authentication authentication) {
        UserAccount user = currentUser(authentication);
        if (user.getRole() == Role.GESTOR_COMUNAL && user.getComuna() != null && !user.getComuna().isBlank()) {
            return new ApiResponse<>(true, "Comuna asociada", List.of(user.getComuna()));
        }
        if (user.getRole() == Role.VISOR_APS && user.getEstablecimientoCodigo() != null) {
            return new ApiResponse<>(true, "Comuna del establecimiento", List.of(service.comunaEstablecimiento(user.getEstablecimientoCodigo())));
        }
        return new ApiResponse<>(true, "Comunas disponibles", service.getComunas());
    }

    @GetMapping("/red")
    public ApiResponse<RedUrgenciaResponse> getRed(@RequestParam(required = false) String comuna,
                                                   @RequestParam(required = false) Integer establecimiento,
                                                   Authentication authentication) {
        UserAccount user = currentUser(authentication);
        String scope = comuna;

        // Gestor comunal: el alcance siempre queda amarrado a su comuna, aunque altere parámetros.
        if (user.getRole() == Role.GESTOR_COMUNAL && user.getComuna() != null && !user.getComuna().isBlank()) {
            scope = user.getComuna();
        // Monitor físico: la comuna se deriva siempre del establecimiento asociado a la cuenta.
        } else if (user.getRole() == Role.VISOR_APS && user.getEstablecimientoCodigo() != null) {
            scope = service.comunaEstablecimiento(user.getEstablecimientoCodigo());
        // ADMIN en Perfil 1: la Vista 2 se deriva del establecimiento seleccionado, no de un filtro regional.
        } else if (establecimiento != null) {
            scope = service.comunaEstablecimiento(establecimiento);
        }

        return new ApiResponse<>(true, "Red de urgencia generada", service.getRedUrgencia(scope));
    }

    private UserAccount currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return users.findByUsername(authentication.getName()).orElseThrow();
    }
}
