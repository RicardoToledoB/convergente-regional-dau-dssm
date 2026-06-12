package cl.dssm.dau.entity;

import cl.dssm.dau.model.EstadoProcesamiento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dau_eventos_recibidos", indexes = {
        @Index(name = "idx_evento_id_dau", columnList = "idDau"),
        @Index(name = "idx_evento_id_atencion", columnList = "idAtencion"),
        @Index(name = "idx_evento_hash", columnList = "hashPayload", unique = true),
        @Index(name = "idx_evento_fecha", columnList = "fechaRecepcion")
})
@Getter
@Setter
public class DauEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80)
    private String idDau;

    @Column(length = 80)
    private String idAtencion;

    @Column(length = 40)
    private String tipoEventoInferido;

    @Column(length = 255)
    private String nombreArchivo;

    @Column(length = 64, nullable = false, unique = true)
    private String hashPayload;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String payloadOriginalJson;

    @Column(nullable = false)
    private LocalDateTime fechaRecepcion;

    @Column(length = 80)
    private String ipOrigen;

    @Column(length = 80)
    private String apiKeyOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoProcesamiento estadoProcesamiento;

    @Column(length = 1200)
    private String mensajeError;
}
