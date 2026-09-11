package cl.dssm.dau.dto;

public record SinEventosEstablecimientoResponse(
        Integer codigoEstablecimiento,
        String establecimiento,
        long total
) {}
