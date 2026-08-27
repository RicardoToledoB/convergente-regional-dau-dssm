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

        String idDau = text(payload, "idDAU");
        String idAtencion = text(payload, "idAtencion");

        // Normativo convergente: idDAU obligatorio, idAtencion opcional.
        // Si idAtencion viene vacío, se usa idDAU como identificador temporal.
        if (isBlank(idDau)) {
            throw new IllegalArgumentException("Campo obligatorio idDAU no puede venir vacio");
        }
        if (isBlank(idAtencion)) {
            idAtencion = idDau;
        }

        final String idDauFinal = idDau;
        final String idAtencionFinal = idAtencion;
        final String tipo = inferTipoEvento(payload, fileName);

        // Si el evento ya fue recibido, no se duplica en bitácora, pero se reejecuta
        // la consolidación idempotente. Esto permite corregir casos ya recibidos donde
        // la admisión llegó con idAtencion nulo y luego llegó el idAtencion real.
        var duplicate = eventRepository.findByHashPayload(hash);
        if (duplicate.isPresent()) {
            DauAttentionEntity att = resolveAttentionForConsolidation(idDauFinal, idAtencionFinal);
            merge(att, payload);
            att.setEstadoActual(resolveEstado(att, tipo));
            att.setFechaUltimoEvento(LocalDateTime.now());
            att.setFechaActualizacion(LocalDateTime.now());
            attentionRepository.save(att);

            DauEventEntity e = duplicate.get();
            return new DauIngestionResponse(idDauFinal, idAtencionFinal, tipo, att.getEstadoActual().name(), hash, "DUPLICADO");
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
            event.setIdDau(idDauFinal);
            event.setIdAtencion(idAtencionFinal);
            event.setTipoEventoInferido(tipo);

            DauAttentionEntity att = resolveAttentionForConsolidation(idDauFinal, idAtencionFinal);
            merge(att, payload);
            att.setEstadoActual(resolveEstado(att, tipo));
            att.setFechaUltimoEvento(LocalDateTime.now());
            att.setFechaActualizacion(LocalDateTime.now());
            attentionRepository.save(att);

            event.setEstadoProcesamiento(EstadoProcesamiento.PROCESADO);
            eventRepository.save(event);
            return new DauIngestionResponse(idDauFinal, idAtencionFinal, tipo, att.getEstadoActual().name(), hash, "PROCESADO");
        } catch (Exception ex) {
            event.setEstadoProcesamiento(EstadoProcesamiento.ERROR);
            event.setMensajeError(ex.getMessage());
            eventRepository.save(event);
            throw ex;
        }
    }

    /**
     * Regla RAYEN / Convergente:
     * - La admisión puede llegar sin idAtencion. En ese caso se guarda temporalmente con idAtencion = idDAU.
     * - Si después llega el mismo idDAU con idAtencion real, se adopta el idAtencion real como canónico.
     * - Si ya existen ambos registros, se fusiona la información faltante desde el temporal al real y se elimina el temporal.
     */
    private DauAttentionEntity resolveAttentionForConsolidation(String idDau, String idAtencion) {
        boolean hasCanonicalId = notBlank(idAtencion) && !idAtencion.equals(idDau);

        if (!hasCanonicalId) {
            return attentionRepository.findByIdDauAndIdAtencion(idDau, idDau)
                    .orElseGet(() -> {
                        DauAttentionEntity n = new DauAttentionEntity();
                        n.setIdDau(idDau);
                        n.setIdAtencion(idDau);
                        n.setFechaCreacion(LocalDateTime.now());
                        return n;
                    });
        }

        var canonicalOpt = attentionRepository.findByIdDauAndIdAtencion(idDau, idAtencion);
        var temporaryOpt = attentionRepository.findByIdDauAndIdAtencion(idDau, idDau);

        if (canonicalOpt.isPresent()) {
            DauAttentionEntity canonical = canonicalOpt.get();
            if (temporaryOpt.isPresent() && !temporaryOpt.get().getId().equals(canonical.getId())) {
                DauAttentionEntity temporary = temporaryOpt.get();
                mergeMissingFrom(canonical, temporary);
                eventRepository.relinkTemporaryEvents(idDau, idDau, idAtencion);
                attentionRepository.delete(temporary);
            }
            return canonical;
        }

        if (temporaryOpt.isPresent()) {
            DauAttentionEntity temporary = temporaryOpt.get();
            temporary.setIdAtencion(idAtencion);
            eventRepository.relinkTemporaryEvents(idDau, idDau, idAtencion);
            return temporary;
        }

        DauAttentionEntity n = new DauAttentionEntity();
        n.setIdDau(idDau);
        n.setIdAtencion(idAtencion);
        n.setFechaCreacion(LocalDateTime.now());
        return n;
    }

    private void mergeMissingFrom(DauAttentionEntity target, DauAttentionEntity source) {
        target.setNombreSolucion(keep(target.getNombreSolucion(), source.getNombreSolucion()));
        target.setNumeroProceso(keep(target.getNumeroProceso(), source.getNumeroProceso()));
        target.setMesAtencion(keep(target.getMesAtencion(), source.getMesAtencion()));
        target.setAnoAtencion(keep(target.getAnoAtencion(), source.getAnoAtencion()));
        target.setCodigoSS(keep(target.getCodigoSS(), source.getCodigoSS()));
        target.setCodigoEstablecimiento(keep(target.getCodigoEstablecimiento(), source.getCodigoEstablecimiento()));
        target.setIdBDPersonas(keep(target.getIdBDPersonas(), source.getIdBDPersonas()));
        target.setIdPaciente(keep(target.getIdPaciente(), source.getIdPaciente()));
        target.setRun(keep(target.getRun(), source.getRun()));
        target.setDv(keep(target.getDv(), source.getDv()));
        target.setTipoIdentificacion(keep(target.getTipoIdentificacion(), source.getTipoIdentificacion()));
        target.setFechaNacimiento(keep(target.getFechaNacimiento(), source.getFechaNacimiento()));
        target.setCodSexo(keep(target.getCodSexo(), source.getCodSexo()));
        target.setPrevision(keep(target.getPrevision(), source.getPrevision()));
        target.setClasificacionBeneficiarioFonasa(keep(target.getClasificacionBeneficiarioFonasa(), source.getClasificacionBeneficiarioFonasa()));
        target.setLeyesPrevisionales(keep(target.getLeyesPrevisionales(), source.getLeyesPrevisionales()));
        target.setFechaAdminision(keep(target.getFechaAdminision(), source.getFechaAdminision()));
        target.setHoraAdmision(keep(target.getHoraAdmision(), source.getHoraAdmision()));
        target.setProcedenciaPaciente(keep(target.getProcedenciaPaciente(), source.getProcedenciaPaciente()));
        target.setUnidadAtencion(keep(target.getUnidadAtencion(), source.getUnidadAtencion()));
        target.setMotivoConsulta(keep(target.getMotivoConsulta(), source.getMotivoConsulta()));
        target.setClasificacionConsulta(keep(target.getClasificacionConsulta(), source.getClasificacionConsulta()));
        target.setMedioLlegada(keep(target.getMedioLlegada(), source.getMedioLlegada()));
        target.setNivelEstratoPaciente(keep(target.getNivelEstratoPaciente(), source.getNivelEstratoPaciente()));
        target.setCategorizacionESI(keep(target.getCategorizacionESI(), source.getCategorizacionESI()));
        target.setPrimeraCategorizacion(keep(target.getPrimeraCategorizacion(), source.getPrimeraCategorizacion()));
        target.setFechaPrimeraCategorizacion(keep(target.getFechaPrimeraCategorizacion(), source.getFechaPrimeraCategorizacion()));
        target.setHoraPrimeraCategorizacion(keep(target.getHoraPrimeraCategorizacion(), source.getHoraPrimeraCategorizacion()));
        target.setTituloProfosionalPrimeraCategorizacion(keep(target.getTituloProfosionalPrimeraCategorizacion(), source.getTituloProfosionalPrimeraCategorizacion()));
        target.setUltimaCategorizacion(keep(target.getUltimaCategorizacion(), source.getUltimaCategorizacion()));
        target.setFechaUltimaCategorizacion(keep(target.getFechaUltimaCategorizacion(), source.getFechaUltimaCategorizacion()));
        target.setHoraUltimaCategorizacion(keep(target.getHoraUltimaCategorizacion(), source.getHoraUltimaCategorizacion()));
        target.setProfesionalUltimaCategorizacion(keep(target.getProfesionalUltimaCategorizacion(), source.getProfesionalUltimaCategorizacion()));
        target.setNumCategorizacion(keep(target.getNumCategorizacion(), source.getNumCategorizacion()));
        target.setFechaAtencion(keep(target.getFechaAtencion(), source.getFechaAtencion()));
        target.setHoraAtencion(keep(target.getHoraAtencion(), source.getHoraAtencion()));
        target.setHipotesisDiagnostico(keep(target.getHipotesisDiagnostico(), source.getHipotesisDiagnostico()));
        target.setCodigoDiagnistico(keep(target.getCodigoDiagnistico(), source.getCodigoDiagnistico()));
        target.setTipoCodigoDiagnostico(keep(target.getTipoCodigoDiagnostico(), source.getTipoCodigoDiagnostico()));
        target.setIndicacionFarmacos(keep(target.getIndicacionFarmacos(), source.getIndicacionFarmacos()));
        target.setIdReceta(keep(target.getIdReceta(), source.getIdReceta()));
        target.setSolicitudMediosDiagnostico(keep(target.getSolicitudMediosDiagnostico(), source.getSolicitudMediosDiagnostico()));
        target.setDescripcionMediosDiagnostico(keep(target.getDescripcionMediosDiagnostico(), source.getDescripcionMediosDiagnostico()));
        target.setFechaAlta(keep(target.getFechaAlta(), source.getFechaAlta()));
        target.setHoraAlta(keep(target.getHoraAlta(), source.getHoraAlta()));
        target.setDiagnosticoFinal(keep(target.getDiagnosticoFinal(), source.getDiagnosticoFinal()));
        target.setTipoDiagnostico(keep(target.getTipoDiagnostico(), source.getTipoDiagnostico()));
        target.setCodigoDiagnosticoAltaMedica(keep(target.getCodigoDiagnosticoAltaMedica(), source.getCodigoDiagnosticoAltaMedica()));
        target.setTipoCodDiagnosticoAltaMedica(keep(target.getTipoCodDiagnosticoAltaMedica(), source.getTipoCodDiagnosticoAltaMedica()));
        target.setCondicionCierreAtencion(keep(target.getCondicionCierreAtencion(), source.getCondicionCierreAtencion()));
        target.setPronosticoMedicoLegal(keep(target.getPronosticoMedicoLegal(), source.getPronosticoMedicoLegal()));
        target.setDestinoAlta(keep(target.getDestinoAlta(), source.getDestinoAlta()));
        target.setGes(keep(target.getGes(), source.getGes()));
        target.setPertinencia(keep(target.getPertinencia(), source.getPertinencia()));
        target.setIdProfesionalAlta(keep(target.getIdProfesionalAlta(), source.getIdProfesionalAlta()));
        target.setRunProfesional(keep(target.getRunProfesional(), source.getRunProfesional()));
        target.setDvProfesional(keep(target.getDvProfesional(), source.getDvProfesional()));
        target.setTituloProfesional(keep(target.getTituloProfesional(), source.getTituloProfesional()));
        target.setEspecialidadMedica(keep(target.getEspecialidadMedica(), source.getEspecialidadMedica()));
        target.setEstadoActual(resolveMostAdvancedEstado(target.getEstadoActual(), source.getEstadoActual()));
        target.setFechaCreacion(keep(target.getFechaCreacion(), source.getFechaCreacion()));
        target.setFechaUltimoEvento(keep(target.getFechaUltimoEvento(), source.getFechaUltimoEvento()));
        target.setFechaActualizacion(LocalDateTime.now());
    }

    private DauEstado resolveMostAdvancedEstado(DauEstado current, DauEstado incoming) {
        if (incoming == null) return current;
        if (current == null) return incoming;
        return rank(incoming) > rank(current) ? incoming : current;
    }

    private int rank(DauEstado estado) {
        if (estado == null) return 0;
        return switch (estado) {
            case ADMISION -> 1;
            case CATEGORIZADA -> 2;
            case ATENCION_MEDICA -> 3;
            case ALTA_MEDICA -> 4;
            case ERROR -> 0;
        };
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
        a.setCodigoDiagnistico(keep(a.getCodigoDiagnistico(), firstText(p, "codigoDiagnistico", "codigoDiagnostico")));
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
            if (f.contains("altamedica") || f.contains("alta_medica") || f.contains("04_")) return "04_ALTA_MEDICA";
            if (f.contains("atencionmedica") || f.contains("atencion_medica") || f.contains("03_")) return "03_ATENCION_MEDICA";
            if (f.contains("categorizacion") || f.contains("02_")) return "02_CATEGORIZACION";
            if (f.contains("admision") || f.contains("01_")) return "01_ADMISION";
        }

        // Orden descendente por avance clínico. Alta prevalece sobre atención médica,
        // y atención médica prevalece sobre categorización.
        if (notBlank(text(p, "fechaAlta"))
                || notBlank(text(p, "horaAlta"))
                || notBlank(text(p, "codigoDiagnosticoAltaMedica"))
                || notBlank(text(p, "condicionCierreAtencion"))
                || notBlank(text(p, "destinoAlta"))) {
            return "04_ALTA_MEDICA";
        }

        // RAYEN puede enviar evento de atención médica sin fechaAtencion/horaAtencion,
        // pero con antecedentes clínicos/diagnósticos. Debe inferirse como 03 y no como 02.
        if (hasMedicalAttentionData(p)) {
            return "03_ATENCION_MEDICA";
        }

        if (notBlank(text(p, "primeraCategorizacion"))
                || notBlank(text(p, "ultimaCategorizacion"))
                || "SI".equalsIgnoreCase(text(p, "categorizacionESI"))) {
            return "02_CATEGORIZACION";
        }
        return "01_ADMISION";
    }

    private boolean hasMedicalAttentionData(JsonNode p) {
        return notBlank(text(p, "fechaAtencion"))
                || notBlank(text(p, "horaAtencion"))
                || notBlank(text(p, "hipotesisDiagnostico"))
                || notBlank(firstText(p, "codigoDiagnistico", "codigoDiagnostico"))
                || notBlank(text(p, "tipoCodigoDiagnostico"))
                || notBlank(text(p, "indicacionFarmacos"))
                || notBlank(text(p, "idReceta"))
                || notBlank(text(p, "solicitudMediosDiagnostico"))
                || notBlank(text(p, "descripcionMediosDiagnostico"));
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
