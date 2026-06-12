package cl.dssm.dau.entity;

import cl.dssm.dau.model.DauEstado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dau_atenciones_consolidadas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dau_atencion", columnNames = {"idDau", "idAtencion"})
}, indexes = {
        @Index(name = "idx_atencion_estado", columnList = "estadoActual"),
        @Index(name = "idx_atencion_establecimiento", columnList = "codigoEstablecimiento"),
        @Index(name = "idx_atencion_actualizacion", columnList = "fechaActualizacion")
})
@Getter
@Setter
public class DauAttentionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String idDau;

    @Column(nullable = false, length = 80)
    private String idAtencion;

    @Column(length = 120)
    private String nombreSolucion;

    private Integer numeroProceso;
    private Integer mesAtencion;
    private Integer anoAtencion;

    @Column(length = 10)
    private String codigoSS;

    private Integer codigoEstablecimiento;

    @Column(length = 80)
    private String idBDPersonas;

    @Column(length = 80)
    private String idPaciente;

    @Column(length = 30)
    private String run;

    @Column(length = 5)
    private String dv;

    private Integer tipoIdentificacion;

    @Column(length = 8)
    private String fechaNacimiento;

    @Column(length = 5)
    private String codSexo;

    @Column(length = 20)
    private String prevision;

    @Column(length = 20)
    private String clasificacionBeneficiarioFonasa;

    @Column(length = 50)
    private String leyesPrevisionales;

    @Column(length = 8)
    private String fechaAdminision;

    @Column(length = 5)
    private String horaAdmision;

    @Column(length = 50)
    private String procedenciaPaciente;

    @Column(length = 20)
    private String unidadAtencion;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(length = 50)
    private String clasificacionConsulta;

    private Integer medioLlegada;

    @Column(length = 50)
    private String nivelEstratoPaciente;

    @Column(length = 5)
    private String categorizacionESI;

    @Column(length = 20)
    private String primeraCategorizacion;

    @Column(length = 8)
    private String fechaPrimeraCategorizacion;

    @Column(length = 5)
    private String horaPrimeraCategorizacion;

    @Column(length = 30)
    private String tituloProfosionalPrimeraCategorizacion;

    @Column(length = 20)
    private String ultimaCategorizacion;

    @Column(length = 8)
    private String fechaUltimaCategorizacion;

    @Column(length = 5)
    private String horaUltimaCategorizacion;

    @Column(length = 30)
    private String profesionalUltimaCategorizacion;

    @Column(length = 20)
    private String numCategorizacion;

    @Column(length = 8)
    private String fechaAtencion;

    @Column(length = 5)
    private String horaAtencion;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String hipotesisDiagnostico;

    @Column(length = 50)
    private String codigoDiagnistico;

    @Column(length = 20)
    private String tipoCodigoDiagnostico;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String indicacionFarmacos;

    @Column(length = 120)
    private String idReceta;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String solicitudMediosDiagnostico;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String descripcionMediosDiagnostico;

    @Column(length = 8)
    private String fechaAlta;

    @Column(length = 5)
    private String horaAlta;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String diagnosticoFinal;

    @Column(length = 20)
    private String tipoDiagnostico;

    @Column(length = 50)
    private String codigoDiagnosticoAltaMedica;

    @Column(length = 20)
    private String tipoCodDiagnosticoAltaMedica;

    @Column(length = 20)
    private String condicionCierreAtencion;

    @Column(length = 20)
    private String pronosticoMedicoLegal;

    @Column(length = 20)
    private String destinoAlta;

    @Column(length = 5)
    private String ges;

    @Column(length = 5)
    private String pertinencia;

    @Column(length = 80)
    private String idProfesionalAlta;

    @Column(length = 30)
    private String runProfesional;

    @Column(length = 5)
    private String dvProfesional;

    @Column(length = 20)
    private String tituloProfesional;

    @Column(length = 20)
    private String especialidadMedica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DauEstado estadoActual = DauEstado.ADMISION;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoEvento;
    private LocalDateTime fechaActualizacion;
}
