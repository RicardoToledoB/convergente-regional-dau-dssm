package cl.dssm.dau.reports;

import java.util.ArrayList;
import java.util.List;

public class ReporteFiltroRequest {
    private List<String> dispositivos = new ArrayList<>();
    private List<Integer> establecimientos = new ArrayList<>();
    private List<String> sexos = new ArrayList<>();
    private List<String> categorias = new ArrayList<>();
    private List<String> origenes = new ArrayList<>();
    private List<String> gruposDiagnostico = new ArrayList<>();
    private List<String> tramosHorarios = new ArrayList<>();
    private Integer edadDesde;
    private Integer edadHasta;
    private String fechaDesde; // yyyy-MM-dd
    private String fechaHasta; // yyyy-MM-dd
    private String agruparPor = "DIA"; // DIA, SEMANA, MES
    private int page = 0;
    private int size = 20;

    public List<String> getDispositivos() { return dispositivos; }
    public void setDispositivos(List<String> dispositivos) { this.dispositivos = dispositivos; }
    public List<Integer> getEstablecimientos() { return establecimientos; }
    public void setEstablecimientos(List<Integer> establecimientos) { this.establecimientos = establecimientos; }
    public List<String> getSexos() { return sexos; }
    public void setSexos(List<String> sexos) { this.sexos = sexos; }
    public List<String> getCategorias() { return categorias; }
    public void setCategorias(List<String> categorias) { this.categorias = categorias; }
    public List<String> getOrigenes() { return origenes; }
    public void setOrigenes(List<String> origenes) { this.origenes = origenes; }
    public List<String> getGruposDiagnostico() { return gruposDiagnostico; }
    public void setGruposDiagnostico(List<String> gruposDiagnostico) { this.gruposDiagnostico = gruposDiagnostico; }
    public List<String> getTramosHorarios() { return tramosHorarios; }
    public void setTramosHorarios(List<String> tramosHorarios) { this.tramosHorarios = tramosHorarios; }
    public Integer getEdadDesde() { return edadDesde; }
    public void setEdadDesde(Integer edadDesde) { this.edadDesde = edadDesde; }
    public Integer getEdadHasta() { return edadHasta; }
    public void setEdadHasta(Integer edadHasta) { this.edadHasta = edadHasta; }
    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }
    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }
    public String getAgruparPor() { return agruparPor; }
    public void setAgruparPor(String agruparPor) { this.agruparPor = agruparPor; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(page, 0); }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size <= 0 ? 20 : Math.min(size, 500); }
}
