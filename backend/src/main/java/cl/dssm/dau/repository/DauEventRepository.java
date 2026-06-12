package cl.dssm.dau.repository;

import cl.dssm.dau.entity.DauEventEntity;
import cl.dssm.dau.model.EstadoProcesamiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DauEventRepository extends JpaRepository<DauEventEntity, Long> {
    Optional<DauEventEntity> findByHashPayload(String hashPayload);
    Page<DauEventEntity> findByEstadoProcesamiento(EstadoProcesamiento estado, Pageable pageable);
    Page<DauEventEntity> findByIdDauAndIdAtencion(String idDau, String idAtencion, Pageable pageable);

    @Query(value = """
            SELECT * FROM dau_eventos_recibidos e
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(e.id_dau) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.id_atencion) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.nombre_archivo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.hash_payload) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:idDau IS NULL OR :idDau = '' OR e.id_dau = :idDau)
              AND (:tipoEvento IS NULL OR :tipoEvento = '' OR e.tipo_evento_inferido = :tipoEvento)
              AND (:estado IS NULL OR :estado = '' OR e.estado_procesamiento = :estado)
              AND (:fechaDesde IS NULL OR DATE(e.fecha_recepcion) >= :fechaDesde)
              AND (:fechaHasta IS NULL OR DATE(e.fecha_recepcion) <= :fechaHasta)
            ORDER BY e.fecha_recepcion DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM dau_eventos_recibidos e
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(e.id_dau) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.id_atencion) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.nombre_archivo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.hash_payload) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:idDau IS NULL OR :idDau = '' OR e.id_dau = :idDau)
              AND (:tipoEvento IS NULL OR :tipoEvento = '' OR e.tipo_evento_inferido = :tipoEvento)
              AND (:estado IS NULL OR :estado = '' OR e.estado_procesamiento = :estado)
              AND (:fechaDesde IS NULL OR DATE(e.fecha_recepcion) >= :fechaDesde)
              AND (:fechaHasta IS NULL OR DATE(e.fecha_recepcion) <= :fechaHasta)
            """,
            nativeQuery = true)
    Page<DauEventEntity> searchAdvanced(@Param("q") String q,
                                        @Param("idDau") String idDau,
                                        @Param("tipoEvento") String tipoEvento,
                                        @Param("estado") String estado,
                                        @Param("fechaDesde") String fechaDesde,
                                        @Param("fechaHasta") String fechaHasta,
                                        Pageable pageable);
}
