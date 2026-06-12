package cl.dssm.dau.dto;

public record DauIngestionResponse(
        String idDau,
        String idAtencion,
        String tipoEventoInferido,
        String estadoAtencion,
        String hashPayload,
        String resultado
) {}
