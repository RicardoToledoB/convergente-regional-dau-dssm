package cl.dssm.dau.dto;

import cl.dssm.dau.model.EstadoProcesamiento;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * Evento DAU expuesto por la API de consulta de paciente.
 *
 * Se omiten deliberadamente metadatos internos sensibles de transporte
 * (hash, IP de origen, API key, etc.) y se conserva el payload clínico
 * original recibido desde el sistema fuente.
 */
public record PatientEventResponse(
        Long id,
        String idDau,
        String idAtencion,
        String tipoEvento,
        LocalDateTime fechaRecepcion,
        EstadoProcesamiento estadoProcesamiento,
        JsonNode payload
) {
}
