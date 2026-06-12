package cl.dssm.dau.reports;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
public class ReportesGestionController {
    private final ReportesGestionService service;

    public ReportesGestionController(ReportesGestionService service) {
        this.service = service;
    }

    @PostMapping("/consultas-red/demanda/buscar")
    public ReporteResponse demanda(@RequestBody ReporteFiltroRequest filtro) {
        return service.consultasRedDemanda(filtro);
    }

    @PostMapping("/consultas-red/atenciones/buscar")
    public ReporteResponse atenciones(@RequestBody ReporteFiltroRequest filtro) {
        return service.consultasRedAtenciones(filtro);
    }

    @PostMapping("/tiempos-espera/categorizacion/buscar")
    public ReporteResponse tiempoCategorizacion(@RequestBody ReporteFiltroRequest filtro) {
        return service.tiemposEspera(filtro, "CATEGORIZACION");
    }

    @PostMapping("/tiempos-espera/atencion/buscar")
    public ReporteResponse tiempoAtencion(@RequestBody ReporteFiltroRequest filtro) {
        return service.tiemposEspera(filtro, "ATENCION");
    }

    @PostMapping("/tiempos-espera/egreso/buscar")
    public ReporteResponse tiempoEgreso(@RequestBody ReporteFiltroRequest filtro) {
        return service.tiemposEspera(filtro, "EGRESO");
    }

    @PostMapping(value = "/{modulo}/{submodulo}/exportar-csv", produces = "text/csv")
    public ResponseEntity<String> exportarCsv(@PathVariable String modulo, @PathVariable String submodulo, @RequestBody ReporteFiltroRequest filtro) {
        String csv = service.exportarCsv(modulo, submodulo, filtro);
        String filename = modulo + "-" + submodulo + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}
