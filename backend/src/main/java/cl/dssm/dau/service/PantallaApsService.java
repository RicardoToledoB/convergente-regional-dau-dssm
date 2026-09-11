package cl.dssm.dau.service;

import cl.dssm.dau.dto.PantallaApsResponse;
import cl.dssm.dau.dto.EstablecimientoPantallaResponse;
import cl.dssm.dau.entity.DauAttentionEntity;
import cl.dssm.dau.model.DauEstado;
import cl.dssm.dau.repository.DauAttentionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PantallaApsService {
    private final DauAttentionRepository attentions;

    private static final DateTimeFormatter FECHA_DAU = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public List<EstablecimientoPantallaResponse> getEstablecimientos() {
        return attentions.findDistinctCodigosEstablecimiento().stream()
                .filter(Objects::nonNull)
                .map(codigo -> new EstablecimientoPantallaResponse(codigo, displayEstablecimiento(codigo)))
                .toList();
    }

    public PantallaApsResponse getPantalla(Integer codigoEstablecimiento) {
        List<DauAttentionEntity> candidatos = codigoEstablecimiento == null
                ? attentions.findByEstadoActualNotOrderByFechaActualizacionDesc(DauEstado.ALTA_MEDICA)
                : attentions.findByCodigoEstablecimientoAndEstadoActualNotOrderByFechaActualizacionDesc(codigoEstablecimiento, DauEstado.ALTA_MEDICA);

        LocalDateTime now = LocalDateTime.now();
        List<DauAttentionEntity> activos = candidatos.stream()
                .filter(a -> esActivoOperacional(a, now))
                .toList();

        long enAtencion = activos.stream().filter(this::estaEnAtencion).count();
        long enEspera = activos.stream().filter(this::estaEnEspera).count();

        List<Long> minutos = activos.stream()
                .filter(this::estaEnEspera)
                .map(a -> minutosDesdeAdmision(a, now))
                .filter(Objects::nonNull)
                .toList();

        long promedio = minutos.isEmpty() ? 0 : Math.round(minutos.stream().mapToLong(Long::longValue).average().orElse(0));
        long maximo = minutos.stream().mapToLong(Long::longValue).max().orElse(0);

        List<PantallaApsResponse.CategoriaTiempo> categorias = List.of("01", "02", "03", "04", "05", "SC").stream()
                .map(cat -> categoriaTiempo(cat, activos, now))
                .toList();

        List<PantallaApsResponse.PacientePantalla> pacientes = activos.stream()
                .sorted(Comparator.comparing((DauAttentionEntity a) -> categoriaOrden(categoria(a))).thenComparing((DauAttentionEntity a) -> Optional.ofNullable(minutosDesdeAdmision(a, now)).orElse(0L), Comparator.reverseOrder()))
                .map(a -> new PantallaApsResponse.PacientePantalla(
                        displayCategoriaCodigo(categoria(a)),
                        displayCategoriaNombre(categoria(a)),
                        formatoMinutos(minutosDesdeAdmision(a, now)),
                        null,
                        estaEnAtencion(a) ? "En atención" : "Categorizado"
                ))
                .toList();

        return new PantallaApsResponse(
                codigoEstablecimiento,
                codigoEstablecimiento == null ? "Red completa" : displayEstablecimiento(codigoEstablecimiento),
                now.format(FECHA_HORA),
                enEspera,
                enAtencion,
                formatoMinutos(promedio),
                formatoMinutos(maximo),
                categorias,
                pacientes,
                distribucion(activos, a -> String.valueOf(a.getCodigoEstablecimiento()), c -> displayEstablecimiento(toInt(c))),
                distribucion(activos, a -> displayCategoriaCodigo(categoria(a)), c -> c + " · " + displayCategoriaNombre(codigoDesdeDisplay(c))),
                distribucion(activos, a -> tramoHorario(a), c -> displayTramo(c))
        );
    }


    /**
     * Vigencia operacional estable para Pantalla APS (v4.4.8).
     *
     * La pantalla sigue el último estado consolidado recibido desde RAYEN y aplica
     * únicamente una ventana máxima de seguridad de 24 horas para impedir que
     * registros abiertos históricos, sin evento de cierre, permanezcan indefinidamente.
     *
     * No modifica datos clínicos ni genera altas artificiales.
     */
    private boolean esActivoOperacional(DauAttentionEntity a, LocalDateTime now) {
        if (a == null || a.getEstadoActual() == null) return false;
        if (a.getEstadoActual() == DauEstado.ALTA_MEDICA || a.getEstadoActual() == DauEstado.ERROR) return false;
        if (a.getFechaAlta() != null && !a.getFechaAlta().isBlank()) return false;

        LocalDateTime adm = parseFechaHora(a.getFechaAdminision(), a.getHoraAdmision());
        if (adm == null || adm.isAfter(now)) return false;

        // ADMISION temporal: mientras RAYEN aún no asigne un idAtencion real.
        if (a.getEstadoActual() == DauEstado.ADMISION
                && !Objects.equals(trimToNull(a.getIdAtencion()), trimToNull(a.getIdDau()))) {
            return false;
        }

        // Red de seguridad: actividad operacional dentro de las últimas 24 horas.
        LocalDateTime referencia = a.getFechaUltimoEvento() != null ? a.getFechaUltimoEvento() : adm;
        if (referencia.isAfter(now)) return false;
        long minutos = Duration.between(referencia, now).toMinutes();
        return minutos >= 0 && minutos <= 24 * 60;
    }

    private boolean estaEnAtencion(DauAttentionEntity a) {
        if (a == null) return false;
        if (a.getFechaAlta() != null && !a.getFechaAlta().isBlank()) return false;
        if (a.getEstadoActual() == DauEstado.ATENCION_MEDICA) return true;
        return (a.getFechaAtencion() != null && !a.getFechaAtencion().isBlank())
                || (a.getHoraAtencion() != null && !a.getHoraAtencion().isBlank());
    }

    private boolean estaEnEspera(DauAttentionEntity a) {
        if (a == null || estaEnAtencion(a)) return false;
        return a.getEstadoActual() == DauEstado.ADMISION || a.getEstadoActual() == DauEstado.CATEGORIZADA;
    }

    private PantallaApsResponse.CategoriaTiempo categoriaTiempo(String cat, List<DauAttentionEntity> activos, LocalDateTime now) {
        List<DauAttentionEntity> rows = activos.stream()
                .filter(this::estaEnEspera)
                .filter(a -> Objects.equals(normalizaCategoria(categoria(a)), cat))
                .toList();
        long max = rows.stream().map(a -> minutosDesdeAdmision(a, now)).filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(0);
        return new PantallaApsResponse.CategoriaTiempo(displayCategoriaCodigo(cat), displayCategoriaNombre(cat), formatoMinutos(max), rows.size());
    }

    private interface KeyFn { String key(DauAttentionEntity a); }
    private interface LabelFn { String label(String key); }
    private List<PantallaApsResponse.Distribucion> distribucion(List<DauAttentionEntity> rows, KeyFn keyFn, LabelFn labelFn) {
        Map<String, Long> map = rows.stream().collect(Collectors.groupingBy(a -> Optional.ofNullable(keyFn.key(a)).orElse("S/D"), LinkedHashMap::new, Collectors.counting()));
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new PantallaApsResponse.Distribucion(e.getKey(), labelFn.label(e.getKey()), e.getValue()))
                .toList();
    }

    private String categoria(DauAttentionEntity a) { return first(a.getUltimaCategorizacion(), a.getPrimeraCategorizacion(), "SC"); }
    private String normalizaCategoria(String c) {
        if (c == null || c.isBlank()) return "SC";
        String v = c.trim().toUpperCase().replace("C", "");
        return switch (v) { case "1", "01" -> "01"; case "2", "02" -> "02"; case "3", "03" -> "03"; case "4", "04" -> "04"; case "5", "05" -> "05"; default -> "SC"; };
    }
    private String displayCategoriaCodigo(String c) { return switch (normalizaCategoria(c)) { case "01" -> "C1"; case "02" -> "C2"; case "03" -> "C3"; case "04" -> "C4"; case "05" -> "C5"; default -> "S/C"; }; }
    private String displayCategoriaNombre(String c) { return switch (normalizaCategoria(c)) { case "01" -> "Riesgo vital"; case "02" -> "Emergencia"; case "03" -> "Urgencia"; case "04" -> "Menor urgencia"; case "05" -> "No urgencia"; default -> "Sin categorización"; }; }
    private int categoriaOrden(String c) { return switch (normalizaCategoria(c)) { case "01" -> 1; case "02" -> 2; case "03" -> 3; case "04" -> 4; case "05" -> 5; default -> 9; }; }
    private String codigoDesdeDisplay(String c) { if (c == null) return "SC"; return c.replace("C", "0").replace("S/0", "SC"); }

    private Long minutosDesdeAdmision(DauAttentionEntity a, LocalDateTime now) {
        LocalDateTime adm = parseFechaHora(a.getFechaAdminision(), a.getHoraAdmision());
        if (adm == null) return null;
        long m = Duration.between(adm, now).toMinutes();
        return Math.max(m, 0);
    }
    private LocalDateTime parseFechaHora(String fecha, String hora) {
        try {
            if (fecha == null || fecha.isBlank()) return null;
            String h = (hora == null || hora.isBlank()) ? "00:00" : hora.trim();
            return LocalDateTime.of(LocalDate.parse(fecha.trim(), FECHA_DAU), LocalTime.parse(h));
        } catch (Exception e) { return null; }
    }
    private String formatoMinutos(Long total) { return total == null ? "--" : formatoMinutos(total.longValue()); }
    private String formatoMinutos(long total) { return String.format("%02d:%02d", total / 60, total % 60); }

    private String tramoHorario(DauAttentionEntity a) {
        if (a == null) return "S/D";
        String hora = first(a.getHoraAdmision(), a.getHoraAtencion());
        if (hora == null || hora.isBlank()) return "S/D";
        try {
            int h = LocalTime.parse(hora.trim()).getHour();
            if (h < 8) return "00-08";
            if (h < 12) return "08-12";
            if (h < 16) return "12-16";
            if (h < 20) return "16-20";
            return "20-24";
        } catch (Exception e) {
            return "S/D";
        }
    }

    private String displayTramo(String t) { return switch (t) { case "08-12" -> "08 a 12"; case "12-16" -> "12 a 16"; case "16-20" -> "16 a 20"; case "20-24" -> "20 a 24"; case "00-08" -> "00 a 08"; default -> "Sin dato"; }; }

    public String displayEstablecimiento(Integer codigo) {
        if (codigo == null) return "Sin establecimiento";

        return switch (codigo) {
            case 126801 -> "126801 - SAR Juan Damianovic";
            case 201079 -> "201079 - SAPU Puerto Natales";
            case 126101 -> "126101 - Hospital Dr. Augusto Essmann Burgos";
            case 126102 -> "126102 - Hospital Dr. Marco Chamorro";
            case 126704 -> "126704 - Hospital Comunitario Cristina Calderón";
            case 126800 -> "126800 - SAPU Dr. Mateo Bencur";
            case 201069 -> "201069 - SAPU Carlos Ibáñez";
            case 126900 -> "126900 - SAPU 18 de Septiembre";
            case 126100 -> "126100 - Hospital Clínico Magallanes";
            case 121105 -> "121105 - Puerto Natales";
            case 121110, 121102 -> codigo + " - Porvenir";
            case 121120, 121108 -> codigo + " - Puerto Williams";
            default -> String.valueOf(codigo);
        };
    }

    private Integer toInt(String v) { try { return Integer.valueOf(v); } catch(Exception e) { return null; } }
    private String trimToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String first(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v; return null; }
}
