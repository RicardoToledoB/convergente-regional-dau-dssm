package cl.dssm.dau.dto;

import java.util.List;

public record PantallaApsAuditoriaResponse(
        String alcance,
        String grupo,
        String categoria,
        long total,
        List<PacienteAuditoria> pacientes
) {
    public record PacienteAuditoria(
            String rut,
            String idDau,
            String idAtencion,
            Integer codigoEstablecimiento,
            String establecimiento,
            String estado,
            String categoria,
            String fechaAdmision,
            String horaAdmision
    ) {}
}
