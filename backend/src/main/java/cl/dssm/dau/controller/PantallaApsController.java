package cl.dssm.dau.controller;

import cl.dssm.dau.dto.ApiResponse;
import cl.dssm.dau.dto.PantallaApsResponse;
import cl.dssm.dau.dto.EstablecimientoPantallaResponse;

import java.util.List;
import cl.dssm.dau.service.PantallaApsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pantallas/aps")
@PreAuthorize("hasAnyRole('ADMIN','VISOR_APS')")
@RequiredArgsConstructor
public class PantallaApsController {
    private final PantallaApsService service;

    @GetMapping
    public ApiResponse<PantallaApsResponse> getPantalla(@RequestParam(required = false) Integer establecimiento) {
        return new ApiResponse<>(true, "Pantalla APS generada", service.getPantalla(establecimiento));
    }

    @GetMapping("/establecimientos")
    public ApiResponse<List<EstablecimientoPantallaResponse>> getEstablecimientos() {
        return new ApiResponse<>(true, "Establecimientos disponibles", service.getEstablecimientos());
    }
}
