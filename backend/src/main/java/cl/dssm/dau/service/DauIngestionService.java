package cl.dssm.dau.service;

import cl.dssm.dau.dto.DauIngestionResponse;
import cl.dssm.dau.entity.DauAttentionEntity;
import cl.dssm.dau.entity.DauEventEntity;
import cl.dssm.dau.model.DauEstado;
import cl.dssm.dau.model.EstadoProcesamiento;
import cl.dssm.dau.repository.DauAttentionRepository;
import cl.dssm.dau.repository.DauEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DauIngestionService {
    private final DauEventRepository eventRepository;
    private final DauAttentionRepository attentionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DauIngestionResponse ingest(JsonNode payload, String fileName, HttpServletRequest request) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON invalido");
        }

        String hash = sha256(json);
        var duplicate = eventRepository.findByHashPayload(hash);
        if (duplicate.isPresent()) {
            DauEventEntity e = duplicate.get();
            return new DauIngestionResponse(e.getIdDau(), e.getIdAtencion(), e.getTipoEventoInferido(), null, hash, "DUPLICADO");
        }

        DauEventEntity event = new DauEventEntity();
        event.setPayloadOriginalJson(json);
        event.setHashPayload(hash);
        event.setNombreArchivo(fileName);
        event.setFechaRecepcion(LocalDateTime.now());
        event.setIpOrigen(resolveIp(request));
        event.setApiKeyOrigen(mask(request.getHeader("X-API-KEY")));
        event.setEstadoProcesamiento(EstadoProcesamiento.RECIBIDO);

        try {
            String idDau = text(payload, "idDAU");
            String idAtencion = text(payload, "idAtencion");
            if (isBlank(idDau) || isBlank(idAtencion)) {
                throw new IllegalArgumentException("Campos obligatorios idDAU e idAtencion no pueden venir vacios");
            }
            event.setIdDau(idDau);
            event.setIdAtencion(idAtencion);
            String tipo = inferTipoEvento(payload, fileName);
            event.setTipoEventoInferido(tipo);

            DauAttentionEntity att = attentionRepository.findByIdDauAndIdAtencion(idDau, idAtencion)
                    .orElseGet(() -> {
                        DauAttentionEntity n = new DauAttentionEntity();
                        n.setIdDau(idDau);
                        n.setIdAtencion(idAtencion);
                        n.setFechaCreacion(LocalDateTime.now());
                        return n;
                    });
            merge(att, payload);
            att.setEstadoActual(resolveEstado(att, tipo));
            att.setFechaUltimoEvento(LocalDateTime.now());
            att.setFechaActualizacion(LocalDateTime.now());
            attentionRepository.save(att);

            event.setEstadoProcesamiento(EstadoProcesamiento.PROCESADO);
            eventRepository.save(event);
            return new DauIngestionResponse(idDau, idAtencion, tipo, att.getEstadoActual().name(), hash, "PROCESADO");
        } catch (Exception ex) {
            event.setEstadoProcesamiento(EstadoProcesamiento.ERROR);
            event.setMensajeError(ex.getMessage());
            eventRepository.save(event);
            throw ex;
        }
    }

    private void merge(DauAttentionEntity a, JsonNode p) {
        a.setNombreSolucion(keep(a.getNombreSolucion(), text(p, "nombreSolucion")));
        a.setNumeroProceso(keep(a.getNumeroProceso(), integer(p, "numeroProceso")));
        a.setMesAtencion(keep(a.getMesAtencion(), integer(p, "mesAtencion")));
        a.setAnoAtencion(keep(a.getAnoAtencion(), integer(p, "anoAtencion")));
        a.setCodigoSS(keep(a.getCodigoSS(), text(p, "codigoSS")));
        a.setCodigoEstablecimiento(keep(a.getCodigoEstablecimiento(), integer(p, "codigoEstablecimiento")));

        a.setIdBDPersonas(keep(a.getIdBDPersonas(), text(p, "idBDPersonas")));
        a.setIdPaciente(keep(a.getIdPaciente(), text(p, "idPaciente")));
        a.setRun(keep(a.getRun(), text(p, "run")));
        a.setDv(keep(a.getDv(), text(p, "dv")));
        a.setTipoIdentificacion(keep(a.getTipoIdentificacion(), integer(p, "tipoIdentificacion")));
        a.setFechaNacimiento(keep(a.getFechaNacimiento(), text(p, "fechaNacimiento")));
        a.setCodSexo(keep(a.getCodSexo(), text(p, "codSexo")));

        a.setPrevision(keep(a.getPrevision(), text(p, "prevision")));
        a.setClasificacionBeneficiarioFonasa(keep(a.getClasificacionBeneficiarioFonasa(), text(p, "clasificacionBeneficiarioFonasa")));
        a.setLeyesPrevisionales(keep(a.getLeyesPrevisionales(), text(p, "leyesPrevisionales")));

        a.setFechaAdminision(keep(a.getFechaAdminision(), firstText(p, "fechaAdminision", "fechaAdmision")));
        a.setHoraAdmision(keep(a.getHoraAdmision(), text(p, "horaAdmision")));
        a.setProcedenciaPaciente(keep(a.getProcedenciaPaciente(), text(p, "procedenciaPaciente")));
        a.setUnidadAtencion(keep(a.getUnidadAtencion(), text(p, "unidadAtencion")));
        a.setMotivoConsulta(keep(a.getMotivoConsulta(), text(p, "motivoConsulta")));
        a.setClasificacionConsulta(keep(a.getClasificacionConsulta(), text(p, "clasificacionConsulta")));
        a.setMedioLlegada(keep(a.getMedioLlegada(), integer(p, "medioLlegada")));
        a.setNivelEstratoPaciente(keep(a.getNivelEstratoPaciente(), text(p, "nivelEstratoPaciente")));

        a.setCategorizacionESI(keep(a.getCategorizacionESI(), text(p, "categorizacionESI")));
        a.setPrimeraCategorizacion(keep(a.getPrimeraCategorizacion(), text(p, "primeraCategorizacion")));
        a.setFechaPrimeraCategorizacion(keep(a.getFechaPrimeraCategorizacion(), text(p, "fechaPrimeraCategorizacion")));
        a.setHoraPrimeraCategorizacion(keep(a.getHoraPrimeraCategorizacion(), text(p, "horaPrimeraCategorizacion")));
        a.setTituloProfosionalPrimeraCategorizacion(keep(a.getTituloProfosionalPrimeraCategorizacion(), text(p, "tituloProfosionalPrimeraCategorizacion")));
        a.setUltimaCategorizacion(keep(a.getUltimaCategorizacion(), text(p, "ultimaCategorizacion")));
        a.setFechaUltimaCategorizacion(keep(a.getFechaUltimaCategorizacion(), text(p, "fechaUltimaCategorizacion")));
        a.setHoraUltimaCategorizacion(keep(a.getHoraUltimaCategorizacion(), text(p, "horaUltimaCategorizacion")));
        a.setProfesionalUltimaCategorizacion(keep(a.getProfesionalUltimaCategorizacion(), text(p, "profesionalUltimaCategorizacion")));
        a.setNumCategorizacion(keep(a.getNumCategorizacion(), text(p, "numCategorizacion")));

        a.setFechaAtencion(keep(a.getFechaAtencion(), text(p, "fechaAtencion")));
        a.setHoraAtencion(keep(a.getHoraAtencion(), text(p, "horaAtencion")));
        a.setHipotesisDiagnostico(keep(a.getHipotesisDiagnostico(), text(p, "hipotesisDiagnostico")));
        a.setCodigoDiagnistico(keep(a.getCodigoDiagnistico(), text(p, "codigoDiagnistico")));
        a.setTipoCodigoDiagnostico(keep(a.getTipoCodigoDiagnostico(), text(p, "tipoCodigoDiagnostico")));
        a.setIndicacionFarmacos(keep(a.getIndicacionFarmacos(), text(p, "indicacionFarmacos")));
        a.setIdReceta(keep(a.getIdReceta(), text(p, "idReceta")));
        a.setSolicitudMediosDiagnostico(keep(a.getSolicitudMediosDiagnostico(), text(p, "solicitudMediosDiagnostico")));
        a.setDescripcionMediosDiagnostico(keep(a.getDescripcionMediosDiagnostico(), text(p, "descripcionMediosDiagnostico")));

        a.setFechaAlta(keep(a.getFechaAlta(), text(p, "fechaAlta")));
        a.setHoraAlta(keep(a.getHoraAlta(), text(p, "horaAlta")));
        a.setDiagnosticoFinal(keep(a.getDiagnosticoFinal(), text(p, "diagnosticoFinal")));
        a.setTipoDiagnostico(keep(a.getTipoDiagnostico(), text(p, "tipoDiagnostico")));
        a.setCodigoDiagnosticoAltaMedica(keep(a.getCodigoDiagnosticoAltaMedica(), text(p, "codigoDiagnosticoAltaMedica")));
        a.setTipoCodDiagnosticoAltaMedica(keep(a.getTipoCodDiagnosticoAltaMedica(), text(p, "tipoCodDiagnosticoAltaMedica")));
        a.setCondicionCierreAtencion(keep(a.getCondicionCierreAtencion(), text(p, "condicionCierreAtencion")));
        a.setPronosticoMedicoLegal(keep(a.getPronosticoMedicoLegal(), text(p, "pronosticoMedicoLegal")));
        a.setDestinoAlta(keep(a.getDestinoAlta(), text(p, "destinoAlta")));
        a.setGes(keep(a.getGes(), text(p, "ges")));
        a.setPertinencia(keep(a.getPertinencia(), text(p, "pertinencia")));
        a.setIdProfesionalAlta(keep(a.getIdProfesionalAlta(), text(p, "idProfesionalAlta")));
        a.setRunProfesional(keep(a.getRunProfesional(), text(p, "runProfesional")));
        a.setDvProfesional(keep(a.getDvProfesional(), text(p, "dvProfesional")));
        a.setTituloProfesional(keep(a.getTituloProfesional(), text(p, "tituloProfesional")));
        a.setEspecialidadMedica(keep(a.getEspecialidadMedica(), text(p, "especialidadMedica")));
    }

    private DauEstado resolveEstado(DauAttentionEntity a, String tipo) {
        if (notBlank(a.getFechaAlta()) || "04_ALTA_MEDICA".equals(tipo)) return DauEstado.ALTA_MEDICA;
        if (notBlank(a.getFechaAtencion()) || "03_ATENCION_MEDICA".equals(tipo)) return DauEstado.ATENCION_MEDICA;
        if (notBlank(a.getPrimeraCategorizacion()) || "02_CATEGORIZACION".equals(tipo)) return DauEstado.CATEGORIZADA;
        return DauEstado.ADMISION;
    }

    private String inferTipoEvento(JsonNode p, String fileName) {
        if (fileName != null) {
            String f = fileName.toLowerCase();
            if (f.contains("altamedica") || f.contains("04_")) return "04_ALTA_MEDICA";
            if (f.contains("atencionmedica") || f.contains("03_")) return "03_ATENCION_MEDICA";
            if (f.contains("categorizacion") || f.contains("02_")) return "02_CATEGORIZACION";
            if (f.contains("admision") || f.contains("01_")) return "01_ADMISION";
        }
        if (notBlank(text(p, "fechaAlta")) || notBlank(text(p, "horaAlta"))) return "04_ALTA_MEDICA";
        if (notBlank(text(p, "fechaAtencion")) || notBlank(text(p, "horaAtencion"))) return "03_ATENCION_MEDICA";
        if (notBlank(text(p, "primeraCategorizacion")) || notBlank(text(p, "ultimaCategorizacion")) || "SI".equalsIgnoreCase(text(p, "categorizacionESI"))) return "02_CATEGORIZACION";
        return "01_ADMISION";
    }

    private String firstText(JsonNode p, String... names) {
        for (String name : names) {
            String value = text(p, name);
            if (notBlank(value)) return value;
        }
        return null;
    }

    private String text(JsonNode p, String name) {
        JsonNode n = p.get(name);
        if (n == null || n.isNull()) return null;
        return n.asText();
    }

    private Integer integer(JsonNode p, String name) {
        JsonNode n = p.get(name);
        if (n == null || n.isNull() || n.asText().isBlank()) return null;
        try { return n.asInt(); } catch (Exception e) { return null; }
    }

    private <T> T keep(T oldValue, T newValue) {
        if (newValue == null) return oldValue;
        if (newValue instanceof String s && s.isBlank()) return oldValue;
        return newValue;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular hash SHA-256", e);
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
    private String mask(String value) {
        if (value == null || value.length() < 6) return "***";
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }
    private boolean isBlank(String v) { return v == null || v.isBlank(); }
    private boolean notBlank(String v) { return !isBlank(v); }
}
