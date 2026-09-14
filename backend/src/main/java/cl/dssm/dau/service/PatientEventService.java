package cl.dssm.dau.service;

import cl.dssm.dau.dto.PatientEventResponse;
import cl.dssm.dau.entity.DauEventEntity;
import cl.dssm.dau.repository.DauAttentionRepository;
import cl.dssm.dau.repository.DauEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PatientEventService {

    private final DauAttentionRepository attentionRepository;
    private final DauEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PatientEventResponse> findEventsByRut(String rutInput) {
        RutParts rut = normalizeRut(rutInput);

        List<String> idDaus = attentionRepository.findDistinctIdDauByPatientRut(rut.run(), rut.dv());
        if (idDaus.isEmpty()) {
            return List.of();
        }

        return eventRepository.findByIdDauInOrderByFechaRecepcionAsc(idDaus).stream()
                .map(this::toResponse)
                .toList();
    }

    private PatientEventResponse toResponse(DauEventEntity event) {
        return new PatientEventResponse(
                event.getId(),
                event.getIdDau(),
                event.getIdAtencion(),
                event.getTipoEventoInferido(),
                event.getFechaRecepcion(),
                event.getEstadoProcesamiento(),
                parsePayload(event.getPayloadOriginalJson())
        );
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            // No se pierde el evento si existiera un registro histórico no parseable.
            return TextNode.valueOf(payload);
        }
    }

    /**
     * Acepta RUT completo con o sin puntos/guion, por ejemplo:
     *  - 6617126-4
     *  - 6.617.126-4
     *  - 66171264
     */
    private RutParts normalizeRut(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RUT es obligatorio");
        }

        String cleaned = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace(".", "")
                .replace("-", "")
                .replace(" ", "");

        if (!cleaned.matches("[0-9]{6,9}[0-9K]")) {
            throw new IllegalArgumentException("Formato de RUT inválido. Debe incluir dígito verificador");
        }

        String run = cleaned.substring(0, cleaned.length() - 1);
        String dv = cleaned.substring(cleaned.length() - 1);

        if (!calculateDv(run).equals(dv)) {
            throw new IllegalArgumentException("Dígito verificador de RUT inválido");
        }

        return new RutParts(stripLeadingZeros(run), dv);
    }

    private String calculateDv(String run) {
        int sum = 0;
        int factor = 2;
        for (int i = run.length() - 1; i >= 0; i--) {
            sum += Character.digit(run.charAt(i), 10) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int result = 11 - (sum % 11);
        if (result == 11) return "0";
        if (result == 10) return "K";
        return String.valueOf(result);
    }

    private String stripLeadingZeros(String run) {
        String stripped = run.replaceFirst("^0+(?!$)", "");
        return stripped.isBlank() ? "0" : stripped;
    }

    private record RutParts(String run, String dv) {
    }
}
