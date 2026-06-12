package cl.dssm.dau.reports;

import java.util.List;
import java.util.Map;

public class ReporteResponse {
    private boolean ok = true;
    private String message = "Reporte generado";
    private Map<String, Object> resumen;
    private List<Map<String, Object>> series;
    private List<Map<String, Object>> detalle;
    private Map<String, Object> distribuciones;
    private Map<String, Object> page;

    public ReporteResponse(Map<String, Object> resumen, List<Map<String, Object>> series, List<Map<String, Object>> detalle, Map<String, Object> page) {
        this(resumen, series, detalle, new java.util.LinkedHashMap<>(), page);
    }

    public ReporteResponse(Map<String, Object> resumen, List<Map<String, Object>> series, List<Map<String, Object>> detalle, Map<String, Object> distribuciones, Map<String, Object> page) {
        this.resumen = resumen;
        this.series = series;
        this.detalle = detalle;
        this.distribuciones = distribuciones;
        this.page = page;
    }

    public boolean isOk() { return ok; }
    public String getMessage() { return message; }
    public Map<String, Object> getResumen() { return resumen; }
    public List<Map<String, Object>> getSeries() { return series; }
    public List<Map<String, Object>> getDetalle() { return detalle; }
    public Map<String, Object> getDistribuciones() { return distribuciones; }
    public Map<String, Object> getPage() { return page; }
}
