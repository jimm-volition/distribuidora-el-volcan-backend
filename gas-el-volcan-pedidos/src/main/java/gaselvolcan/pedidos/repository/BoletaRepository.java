package gaselvolcan.pedidos.repository;

import gaselvolcan.pedidos.model.Boleta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    @EntityGraph(attributePaths="detalles")
    List<Boleta> findAllByOrderByFechaDesc();
    @EntityGraph(attributePaths="detalles")
    List<Boleta> findByClienteCorreoIgnoreCaseOrderByFechaDesc(String correo);
    @Override
    @EntityGraph(attributePaths="detalles")
    Optional<Boleta> findById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Boleta b WHERE b.id = :id")
    Optional<Boleta> bloquearPorId(@Param("id") Long id);
}
