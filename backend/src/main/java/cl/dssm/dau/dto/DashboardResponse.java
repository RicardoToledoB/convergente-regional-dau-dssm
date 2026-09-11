package cl.dssm.dau.dto;

public record DashboardResponse(
        long totalAtenciones,
        long admision,
        long categorizadas,
        long atencionMedica,
        long altaMedica,
        long totalEventos,
        long eventosConError,
        long sinEventos3h,
        long sinEventos6h,
        long sinEventos12h,
        long sinEventos24h
) {}
