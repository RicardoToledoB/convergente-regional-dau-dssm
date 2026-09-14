package cl.dssm.dau.repository;

import cl.dssm.dau.entity.DauAttentionEntity;
import cl.dssm.dau.model.DauEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DauAttentionRepository extends JpaRepository<DauAttentionEntity, Long> {
    Optional<DauAttentionEntity> findByIdDauAndIdAtencion(String idDau, String idAtencion);

    @Query("""
            SELECT DISTINCT a.idDau
            FROM DauAttentionEntity a
            WHERE a.run = :run
              AND (:dv IS NULL OR UPPER(a.dv) = UPPER(:dv))
            ORDER BY a.idDau
            """)
    List<String> findDistinctIdDauByPatientRut(@Param("run") String run, @Param("dv") String dv);

    Page<DauAttentionEntity> findByEstadoActual(DauEstado estado, Pageable pageable);

    @Query(value = """
            SELECT * FROM dau_atenciones_consolidadas a
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(a.id_dau) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.id_atencion) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.id_paciente) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.run) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.motivo_consulta) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:estado IS NULL OR :estado = '' OR a.estado_actual = :estado)
              AND (:establecimiento IS NULL OR a.codigo_establecimiento = :establecimiento)
              AND (:categoria IS NULL OR :categoria = '' OR a.ultima_categorizacion = :categoria OR a.primera_categorizacion = :categoria)
              AND (:fechaDesde IS NULL OR a.fecha_adminision >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha_adminision <= :fechaHasta)
            ORDER BY a.fecha_actualizacion DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM dau_atenciones_consolidadas a
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(a.id_dau) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.id_atencion) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.id_paciente) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.run) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.motivo_consulta) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:estado IS NULL OR :estado = '' OR a.estado_actual = :estado)
              AND (:establecimiento IS NULL OR a.codigo_establecimiento = :establecimiento)
              AND (:categoria IS NULL OR :categoria = '' OR a.ultima_categorizacion = :categoria OR a.primera_categorizacion = :categoria)
              AND (:fechaDesde IS NULL OR a.fecha_adminision >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha_adminision <= :fechaHasta)
            """,
            nativeQuery = true)
    Page<DauAttentionEntity> searchAdvanced(@Param("q") String q,
                                            @Param("estado") String estado,
                                            @Param("establecimiento") Integer establecimiento,
                                            @Param("categoria") String categoria,
                                            @Param("fechaDesde") String fechaDesde,
                                            @Param("fechaHasta") String fechaHasta,
                                            Pageable pageable);

    List<DauAttentionEntity> findByEstadoActualNotOrderByFechaActualizacionDesc(DauEstado estado);
    List<DauAttentionEntity> findByCodigoEstablecimientoAndEstadoActualNotOrderByFechaActualizacionDesc(Integer codigoEstablecimiento, DauEstado estado);

    long countByEstadoActual(DauEstado estado);

    /**
     * DAU operativamente abiertos que no han recibido novedades durante más
     * de N horas. Es un indicador de monitoreo: no modifica estados ni datos.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM dau_atenciones_consolidadas a
            WHERE a.estado_actual IN ('ADMISION', 'CATEGORIZADA', 'ATENCION_MEDICA')
              AND (a.fecha_alta IS NULL OR TRIM(a.fecha_alta) = '')
              AND a.fecha_ultimo_evento IS NOT NULL
              AND TIMESTAMPDIFF(MINUTE, a.fecha_ultimo_evento, NOW()) > (:hours * 60)
            """, nativeQuery = true)
    long countAbiertosSinEventosPorHoras(@Param("hours") int hours);

    @Query(value = """
            SELECT DISTINCT a.codigo_establecimiento
            FROM dau_atenciones_consolidadas a
            WHERE a.codigo_establecimiento IS NOT NULL
            ORDER BY a.codigo_establecimiento
            """, nativeQuery = true)
    List<Integer> findDistinctCodigosEstablecimiento();

    @Query(value = """
            SELECT *
            FROM dau_atenciones_consolidadas a
            WHERE a.estado_actual IN ('ADMISION', 'CATEGORIZADA', 'ATENCION_MEDICA')
              AND (a.fecha_alta IS NULL OR TRIM(a.fecha_alta) = '')
              AND a.fecha_ultimo_evento IS NOT NULL
              AND TIMESTAMPDIFF(MINUTE, a.fecha_ultimo_evento, NOW()) > (:hours * 60)
            ORDER BY a.fecha_ultimo_evento ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM dau_atenciones_consolidadas a
            WHERE a.estado_actual IN ('ADMISION', 'CATEGORIZADA', 'ATENCION_MEDICA')
              AND (a.fecha_alta IS NULL OR TRIM(a.fecha_alta) = '')
              AND a.fecha_ultimo_evento IS NOT NULL
              AND TIMESTAMPDIFF(MINUTE, a.fecha_ultimo_evento, NOW()) > (:hours * 60)
            """, nativeQuery = true)
    Page<DauAttentionEntity> findAbiertosSinEventosPorHoras(@Param("hours") int hours, Pageable pageable);

    @Query(value = """
            SELECT a.codigo_establecimiento, COUNT(*) AS total
            FROM dau_atenciones_consolidadas a
            WHERE a.estado_actual IN ('ADMISION', 'CATEGORIZADA', 'ATENCION_MEDICA')
              AND (a.fecha_alta IS NULL OR TRIM(a.fecha_alta) = '')
              AND a.fecha_ultimo_evento IS NOT NULL
              AND TIMESTAMPDIFF(MINUTE, a.fecha_ultimo_evento, NOW()) > (:hours * 60)
            GROUP BY a.codigo_establecimiento
            ORDER BY total DESC, a.codigo_establecimiento
            """, nativeQuery = true)
    List<Object[]> countAbiertosSinEventosPorEstablecimiento(@Param("hours") int hours);
}
