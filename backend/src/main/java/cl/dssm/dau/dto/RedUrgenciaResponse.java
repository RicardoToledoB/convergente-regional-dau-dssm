package cl.dssm.dau.dto;

import java.util.List;

public record RedUrgenciaResponse(
        String alcance,
        String comuna,
        long totalEnEspera,
        long totalEnAtencion,
        String tiempoPromedioEspera,
        long centrosActivos,
        List<CentroUrgencia> centros
) {
    public record CentroUrgencia(
            Integer codigoEstablecimiento,
            String establecimiento,
            String comuna,
            String c1,
            String c2,
            String c3,
            String c4,
            String c5,
            String sinCategoria,
            long pacientesEnEspera,
            long pacientesEnAtencion,
            String tiempoPromedioEspera
    ) {}
}
