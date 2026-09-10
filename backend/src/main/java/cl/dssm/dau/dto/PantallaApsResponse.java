package cl.dssm.dau.dto;

import java.util.List;

public record PantallaApsResponse(
        Integer codigoEstablecimiento,
        String establecimiento,
        String fechaHoraActualizacion,
        long pacientesEnEspera,
        long pacientesEnAtencion,
        String tiempoPromedioEspera,
        String tiempoMaximoEspera,
        List<CategoriaTiempo> tiemposPorCategoria,
        List<PacientePantalla> pacientes,
        List<Distribucion> distribucionPorEstablecimiento,
        List<Distribucion> distribucionPorCategoria,
        List<Distribucion> distribucionPorTramoHorario
) {
    public record CategoriaTiempo(String categoria, String nombre, String tiempo, long total) {}
    public record PacientePantalla(
            String categoria,
            String categoriaNombre,
            String tiempoTranscurrido,
            String box,
            String estado
    ) {}
    public record Distribucion(String codigo, String nombre, long total) {}
}
