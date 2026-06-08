package cl.dssm.dau.controller;

import cl.dssm.dau.dto.ApiResponse;
import cl.dssm.dau.dto.DashboardResponse;
import cl.dssm.dau.entity.DauAttentionEntity;
import cl.dssm.dau.entity.DauEventEntity;
import cl.dssm.dau.model.DauEstado;
import cl.dssm.dau.model.EstadoProcesamiento;
import cl.dssm.dau.repository.DauAttentionRepository;
import cl.dssm.dau.repository.DauEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dau")
@RequiredArgsConstructor
public class DauMonitoringController {
    private final DauAttentionRepository attentions;
    private final DauEventRepository events;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        var data = new DashboardResponse(
                attentions.count(),
                attentions.countByEstadoActual(DauEstado.ADMISION),
                attentions.countByEstadoActual(DauEstado.CATEGORIZADA),
                attentions.countByEstadoActual(DauEstado.ATENCION_MEDICA),
                attentions.countByEstadoActual(DauEstado.ALTA_MEDICA),
                events.count(),
                events.findByEstadoProcesamiento(EstadoProcesamiento.ERROR, PageRequest.of(0, 1)).getTotalElements()
        );
        return new ApiResponse<>(true, "Dashboard", data);
    }

    @GetMapping("/atenciones")
    public ApiResponse<Page<DauAttentionEntity>> listAttentions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer establecimiento,
            @RequestParam(defaultValue = "") String categoria,
            @RequestParam(defaultValue = "") String fechaDesde,
            @RequestParam(defaultValue = "") String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha_actualizacion"));
        return new ApiResponse<>(true, "Atenciones", attentions.searchAdvanced(q, estado, establecimiento, categoria, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/atenciones/{idDau}/{idAtencion}")
    public ApiResponse<DauAttentionEntity> getAttention(@PathVariable String idDau, @PathVariable String idAtencion) {
        return new ApiResponse<>(true, "Atencion", attentions.findByIdDauAndIdAtencion(idDau, idAtencion).orElseThrow());
    }

    @GetMapping("/atenciones/{idDau}/{idAtencion}/eventos")
    public ApiResponse<Page<DauEventEntity>> eventsByAttention(@PathVariable String idDau, @PathVariable String idAtencion,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaRecepcion"));
        return new ApiResponse<>(true, "Eventos de atencion", events.findByIdDauAndIdAtencion(idDau, idAtencion, pageable));
    }

    @GetMapping("/eventos")
    public ApiResponse<Page<DauEventEntity>> listEvents(@RequestParam(defaultValue = "") String q,
                                                        @RequestParam(defaultValue = "") String idDau,
                                                        @RequestParam(defaultValue = "") String tipoEvento,
                                                        @RequestParam(defaultValue = "") String estado,
                                                        @RequestParam(defaultValue = "") String fechaDesde,
                                                        @RequestParam(defaultValue = "") String fechaHasta,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha_recepcion"));
        return new ApiResponse<>(true, "Eventos", events.searchAdvanced(q, idDau, tipoEvento, estado, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/eventos/{id}")
    public ApiResponse<DauEventEntity> getEvent(@PathVariable Long id) {
        return new ApiResponse<>(true, "Evento", events.findById(id).orElseThrow());
    }

    @GetMapping("/errores")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ApiResponse<Page<DauEventEntity>> errors(@RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "") String idDau,
                                                    @RequestParam(defaultValue = "") String fechaDesde,
                                                    @RequestParam(defaultValue = "") String fechaHasta,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha_recepcion"));
        return new ApiResponse<>(true, "Errores", events.searchAdvanced(q, idDau, "", "ERROR", fechaDesde, fechaHasta, pageable));
    }
}
