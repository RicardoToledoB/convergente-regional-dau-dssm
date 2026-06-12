package cl.dssm.dau.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportesGestionService {
    private static final String TABLE = "dau_atenciones_consolidadas";
    private final NamedParameterJdbcTemplate jdbc;
    private final Set<String> cols;

    public ReportesGestionService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.cols = loadColumns();
    }

    public ReporteResponse consultasRedDemanda(ReporteFiltroRequest filtro) {
        return reporteBase(filtro, false, null);
    }

    public ReporteResponse consultasRedAtenciones(ReporteFiltroRequest filtro) {
        return reporteBase(filtro, true, null);
    }

    public ReporteResponse tiemposEspera(ReporteFiltroRequest filtro, String tipo) {
        return reporteBase(filtro, true, tipo);
    }

    public String exportarCsv(String modulo, String submodulo, ReporteFiltroRequest filtro) {
        filtro.setPage(0);
        filtro.setSize(5000);
        ReporteResponse response;
        if ("consultas-red".equalsIgnoreCase(modulo) && "demanda".equalsIgnoreCase(submodulo)) response = consultasRedDemanda(filtro);
        else if ("consultas-red".equalsIgnoreCase(modulo) && "atenciones".equalsIgnoreCase(submodulo)) response = consultasRedAtenciones(filtro);
        else if ("tiempos-espera".equalsIgnoreCase(modulo)) response = tiemposEspera(filtro, submodulo.toUpperCase(Locale.ROOT));
        else response = consultasRedDemanda(filtro);
        return toCsv(response.getDetalle());
    }

    private ReporteResponse reporteBase(ReporteFiltroRequest filtro, boolean soloFinalizadas, String tiempoTipo) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        applyFiltros(where, p, filtro);
        if (soloFinalizadas && has("fecha_alta")) where.append(" AND fecha_alta IS NOT NULL AND fecha_alta <> '' ");

        String tiempoExpr = tiempoExpr(tiempoTipo);
        String periodoExpr = periodoExpr(filtro.getAgruparPor());
        String selectTiempo = tiempoExpr == null ? "NULL AS minutos_espera," : tiempoExpr + " AS minutos_espera,";

        int limit = filtro.getSize();
        int offset = filtro.getPage() * filtro.getSize();
        p.addValue("limit", limit);
        p.addValue("offset", offset);

        String countSql = "SELECT COUNT(*) FROM " + TABLE + where;
        long total = jdbc.queryForObject(countSql, p, Long.class);

        String resumenSql = "SELECT COUNT(*) total, " +
                (tiempoExpr == null ? "NULL promedio_minutos, NULL minimo_minutos, NULL maximo_minutos" :
                        "ROUND(AVG(" + tiempoExpr + "),1) promedio_minutos, MIN(" + tiempoExpr + ") minimo_minutos, MAX(" + tiempoExpr + ") maximo_minutos") +
                " FROM " + TABLE + where;
        Map<String, Object> resumen = jdbc.queryForMap(resumenSql, p);
        resumen.put("mediana_minutos", tiempoExpr == null ? null : calcularMediana(tiempoExpr, where.toString(), p));

        String seriesSql = "SELECT " + periodoExpr + " periodo, COUNT(*) total " +
                (tiempoExpr == null ? "" : ", ROUND(AVG(" + tiempoExpr + "),1) promedio_minutos ") +
                " FROM " + TABLE + where + " GROUP BY periodo ORDER BY periodo LIMIT 500";
        List<Map<String, Object>> series = jdbc.queryForList(seriesSql, p);

        String detalleSql = "SELECT " +
                sel("id_dau", "idDau") + ", " + sel("id_atencion", "idAtencion") + ", " +
                sel("codigo_establecimiento", "establecimiento") + ", " + sel("estado_actual", "estado") + ", " +
                sel("motivo_consulta", "motivo") + ", " + sel("ultima_categorizacion", "categoria") + ", " +
                sel("cod_sexo", "sexo") + ", " + sel("fecha_nacimiento", "fechaNacimiento") + ", " +
                sel("fecha_adminision", "fechaAdmision") + ", " + sel("hora_admision", "horaAdmision") + ", " +
                sel("fecha_atencion", "fechaAtencion") + ", " + sel("hora_atencion", "horaAtencion") + ", " +
                sel("fecha_alta", "fechaAlta") + ", " + sel("hora_alta", "horaAlta") + ", " +
                sel("codigo_diagnostico_alta_medica", "codigoDiagnosticoAlta") + ", " +
                selectTiempo + " " + periodoExpr + " periodo " +
                " FROM " + TABLE + where + " ORDER BY " + orderColumn() + " DESC LIMIT :limit OFFSET :offset";
        List<Map<String, Object>> detalle = jdbc.queryForList(detalleSql, p);
        Map<String, Object> distribuciones = distribuciones(where.toString(), p);

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("number", filtro.getPage());
        page.put("size", filtro.getSize());
        page.put("totalElements", total);
        page.put("totalPages", (int) Math.ceil(total / (double) filtro.getSize()));
        return new ReporteResponse(resumen, series, detalle, distribuciones, page);
    }

    private Map<String, Object> distribuciones(String where, MapSqlParameterSource baseParams) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("establecimientos", agrupacion(where, baseParams, has("codigo_establecimiento") ? "codigo_establecimiento" : "'Sin dato'", 12));
        out.put("categorias", agrupacion(where, baseParams, has("ultima_categorizacion") ? "COALESCE(NULLIF(ultima_categorizacion,''),'SIN_CATEGORIA')" : "'Sin dato'", 8));
        out.put("sexos", agrupacion(where, baseParams, has("cod_sexo") ? "COALESCE(NULLIF(cod_sexo,''),'SIN_DATO')" : "'Sin dato'", 6));
        if (has("hora_admision")) {
            String tramo = "CASE " +
                    "WHEN TIME(hora_admision) BETWEEN '06:00:00' AND '11:59:59' THEN 'AM' " +
                    "WHEN TIME(hora_admision) BETWEEN '12:00:00' AND '19:59:59' THEN 'PM' " +
                    "WHEN TIME(hora_admision) BETWEEN '20:00:00' AND '23:59:59' THEN 'NOCHE' " +
                    "WHEN TIME(hora_admision) BETWEEN '00:00:00' AND '05:59:59' THEN 'MADRUGADA' " +
                    "ELSE 'SIN_DATO' END";
            out.put("tramosHorarios", agrupacion(where, baseParams, tramo, 8));
        } else {
            out.put("tramosHorarios", List.of());
        }
        if (has("codigo_diagnostico_alta_medica")) {
            String grupo = "CASE " +
                    "WHEN codigo_diagnostico_alta_medica LIKE 'J%' THEN 'RESPIRATORIO' " +
                    "WHEN codigo_diagnostico_alta_medica LIKE 'I%' THEN 'CARDIOVASCULAR' " +
                    "WHEN codigo_diagnostico_alta_medica LIKE 'F%' THEN 'SALUD_MENTAL' " +
                    "WHEN codigo_diagnostico_alta_medica LIKE 'S%' OR codigo_diagnostico_alta_medica LIKE 'T%' THEN 'TRAUMA' " +
                    "WHEN codigo_diagnostico_alta_medica LIKE 'K%' THEN 'DIGESTIVO' " +
                    "ELSE 'OTROS' END";
            out.put("gruposDiagnostico", agrupacion(where, baseParams, grupo, 8));
        } else {
            out.put("gruposDiagnostico", List.of());
        }
        return out;
    }

    private List<Map<String, Object>> agrupacion(String where, MapSqlParameterSource baseParams, String expr, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        baseParams.getValues().forEach(p::addValue);
        p.addValue("distLimit", limit);
        String sql = "SELECT " + expr + " codigo, COUNT(*) total FROM " + TABLE + where + " GROUP BY codigo ORDER BY total DESC, codigo ASC LIMIT :distLimit";
        try { return jdbc.queryForList(sql, p); } catch (Exception e) { return List.of(); }
    }

    private void applyFiltros(StringBuilder where, MapSqlParameterSource p, ReporteFiltroRequest f) {
        if (notEmpty(f.getEstablecimientos()) && has("codigo_establecimiento")) {
            where.append(" AND codigo_establecimiento IN (:establecimientos) "); p.addValue("establecimientos", f.getEstablecimientos());
        }
        if (notEmpty(f.getSexos()) && has("cod_sexo")) {
            where.append(" AND cod_sexo IN (:sexos) "); p.addValue("sexos", f.getSexos());
        }
        if (notEmpty(f.getCategorias()) && has("ultima_categorizacion")) {
            where.append(" AND ultima_categorizacion IN (:categorias) "); p.addValue("categorias", f.getCategorias());
        }
        if (notEmpty(f.getOrigenes()) && has("procedencia_paciente")) {
            where.append(" AND procedencia_paciente IN (:origenes) "); p.addValue("origenes", f.getOrigenes());
        }
        if (notEmpty(f.getGruposDiagnostico()) && has("codigo_diagnostico_alta_medica")) {
            List<String> prefixes = f.getGruposDiagnostico().stream().map(this::grupoToPrefix).flatMap(Collection::stream).toList();
            if (!prefixes.isEmpty()) {
                List<String> likes = new ArrayList<>();
                int i = 0;
                for (String prefix : prefixes) { String key = "diag" + i++; likes.add("codigo_diagnostico_alta_medica LIKE :" + key); p.addValue(key, prefix + "%"); }
                where.append(" AND (" + String.join(" OR ", likes) + ") ");
            }
        }
        String adm = datetimeExpr("fecha_adminision", "hora_admision");
        if (adm != null) {
            if (f.getFechaDesde() != null && !f.getFechaDesde().isBlank()) { where.append(" AND ").append(adm).append(" >= :fechaDesde "); p.addValue("fechaDesde", f.getFechaDesde() + " 00:00:00"); }
            if (f.getFechaHasta() != null && !f.getFechaHasta().isBlank()) { where.append(" AND ").append(adm).append(" <= :fechaHasta "); p.addValue("fechaHasta", f.getFechaHasta() + " 23:59:59"); }
            if (notEmpty(f.getTramosHorarios()) && has("hora_admision")) {
                List<String> tramoSql = new ArrayList<>();
                for (String t : f.getTramosHorarios()) {
                    switch (t.toUpperCase(Locale.ROOT)) {
                        case "AM" -> tramoSql.add("TIME(hora_admision) BETWEEN '06:00:00' AND '11:59:59'");
                        case "PM" -> tramoSql.add("TIME(hora_admision) BETWEEN '12:00:00' AND '19:59:59'");
                        case "NOCHE" -> tramoSql.add("TIME(hora_admision) BETWEEN '20:00:00' AND '23:59:59'");
                        case "MADRUGADA" -> tramoSql.add("TIME(hora_admision) BETWEEN '00:00:00' AND '05:59:59'");
                    }
                }
                if (!tramoSql.isEmpty()) where.append(" AND (" + String.join(" OR ", tramoSql) + ") ");
            }
        }
        if ((f.getEdadDesde() != null || f.getEdadHasta() != null) && has("fecha_nacimiento")) {
            String edadExpr = "TIMESTAMPDIFF(YEAR, STR_TO_DATE(fecha_nacimiento, '%Y%m%d'), CURDATE())";
            if (f.getEdadDesde() != null) { where.append(" AND ").append(edadExpr).append(" >= :edadDesde "); p.addValue("edadDesde", f.getEdadDesde()); }
            if (f.getEdadHasta() != null) { where.append(" AND ").append(edadExpr).append(" <= :edadHasta "); p.addValue("edadHasta", f.getEdadHasta()); }
        }
    }

    private String periodoExpr(String agruparPor) {
        String fechaBase = fechaBaseReporteExpr();
        if (fechaBase == null) return "'SIN_FECHA'";
        String g = agruparPor == null ? "DIA" : agruparPor.toUpperCase(Locale.ROOT);
        return switch (g) {
            case "MES" -> "DATE_FORMAT(" + fechaBase + ", '%Y-%m')";
            case "SEMANA" -> "CONCAT(YEAR(" + fechaBase + "), '-S', LPAD(WEEK(" + fechaBase + ", 3), 2, '0'))";
            default -> "DATE(" + fechaBase + ")";
        };
    }

    private String tiempoExpr(String tipo) {
        if (tipo == null) return null;
        String adm = admisionDateTimeExpr();
        String cat = datetimeExpr("fecha_primera_categorizacion", "hora_primera_categorizacion");
        String aten = datetimeExpr("fecha_atencion", "hora_atencion");
        String alta = datetimeExpr("fecha_alta", "hora_alta");
        return switch (tipo.toUpperCase(Locale.ROOT)) {
            case "CATEGORIZACION" -> adm != null && cat != null ? "GREATEST(TIMESTAMPDIFF(MINUTE, " + adm + ", " + cat + "), 0)" : null;
            case "ATENCION" -> adm != null && aten != null ? "GREATEST(TIMESTAMPDIFF(MINUTE, " + adm + ", " + aten + "), 0)" : null;
            case "EGRESO" -> adm != null && alta != null ? "GREATEST(TIMESTAMPDIFF(MINUTE, " + adm + ", " + alta + "), 0)" : null;
            default -> null;
        };
    }

    /**
     * Fecha base de reportería. Normalmente es fecha/hora de admisión.
     * Si por una integración o prueba antigua llegó horaAdmision pero no fechaAdminision,
     * se reconstruye usando la fecha de atención o alta para no dejar la serie agrupada en blanco.
     */
    private String fechaBaseReporteExpr() {
        String adm = admisionDateTimeExpr();
        String aten = datetimeExpr("fecha_atencion", "hora_atencion");
        String alta = datetimeExpr("fecha_alta", "hora_alta");
        String creacion = has("fecha_creacion") ? "fecha_creacion" : null;
        List<String> parts = new ArrayList<>();
        if (adm != null) parts.add(adm);
        if (aten != null) parts.add(aten);
        if (alta != null) parts.add(alta);
        if (creacion != null) parts.add(creacion);
        if (parts.isEmpty()) return null;
        return "COALESCE(" + String.join(", ", parts) + ")";
    }

    private String admisionDateTimeExpr() {
        if (!has("hora_admision")) return datetimeExpr("fecha_adminision", "hora_admision");
        String normal = datetimeExpr("fecha_adminision", "hora_admision");
        String desdeAtencion = datetimeWithExternalDateExpr("fecha_atencion", "hora_admision");
        String desdeAlta = datetimeWithExternalDateExpr("fecha_alta", "hora_admision");
        List<String> parts = new ArrayList<>();
        if (normal != null) parts.add(normal);
        if (desdeAtencion != null) parts.add(desdeAtencion);
        if (desdeAlta != null) parts.add(desdeAlta);
        if (parts.isEmpty()) return null;
        return "COALESCE(" + String.join(", ", parts) + ")";
    }

    private String datetimeExpr(String fechaCol, String horaCol) {
        return datetimeWithExternalDateExpr(fechaCol, horaCol);
    }

    private String datetimeWithExternalDateExpr(String fechaCol, String horaCol) {
        if (!has(fechaCol) || !has(horaCol)) return null;
        return "STR_TO_DATE(CONCAT(NULLIF(" + fechaCol + ", ''), ' ', COALESCE(NULLIF(" + horaCol + ", ''), '00:00')), '%d%m%Y %H:%i')";
    }

    private BigDecimal calcularMediana(String expr, String where, MapSqlParameterSource baseParams) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        baseParams.getValues().forEach(p::addValue);
        String sql = "SELECT AVG(x.minutos) FROM (" +
                " SELECT " + expr + " minutos, ROW_NUMBER() OVER (ORDER BY " + expr + ") rn, COUNT(*) OVER () cnt " +
                " FROM " + TABLE + where + " AND " + expr + " IS NOT NULL" +
                ") x WHERE x.rn IN (FLOOR((x.cnt + 1)/2), FLOOR((x.cnt + 2)/2))";
        try { return jdbc.queryForObject(sql, p, BigDecimal.class); } catch (Exception e) { return null; }
    }

    private Set<String> loadColumns() {
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = :table";
        return new HashSet<>(jdbc.queryForList(sql, new MapSqlParameterSource("table", TABLE), String.class));
    }

    private boolean has(String col) { return cols.contains(col); }
    private boolean notEmpty(Collection<?> c) { return c != null && !c.isEmpty(); }
    private String sel(String col, String alias) { return has(col) ? col + " AS " + alias : "NULL AS " + alias; }
    private String orderColumn() { return has("fecha_actualizacion") ? "fecha_actualizacion" : has("id") ? "id" : "id_dau"; }

    private List<String> grupoToPrefix(String grupo) {
        if (grupo == null) return List.of();
        return switch (grupo.toUpperCase(Locale.ROOT)) {
            case "RESPIRATORIO" -> List.of("J");
            case "CARDIOVASCULAR" -> List.of("I");
            case "SALUD_MENTAL" -> List.of("F");
            case "TRAUMA" -> List.of("S", "T");
            case "DIGESTIVO" -> List.of("K");
            default -> List.of(grupo);
        };
    }

    private String toCsv(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return "sin_datos\n";
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder(String.join(";", headers)).append('\n');
        for (Map<String, Object> row : rows) {
            sb.append(headers.stream().map(h -> csv(row.get(h))).collect(Collectors.joining(";"))).append('\n');
        }
        return sb.toString();
    }

    private String csv(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
