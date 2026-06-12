package cl.dssm.dau.repository;

import cl.dssm.dau.entity.DauAttentionEntity;
import cl.dssm.dau.model.DauEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DauAttentionRepository extends JpaRepository<DauAttentionEntity, Long> {
    Optional<DauAttentionEntity> findByIdDauAndIdAtencion(String idDau, String idAtencion);

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

    long countByEstadoActual(DauEstado estado);
}
