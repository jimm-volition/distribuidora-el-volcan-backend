package gaselvolcan.pedidos.repository;

import gaselvolcan.pedidos.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByCodigo(String codigo);
    Optional<Producto> findByCodigoAndActivoTrue(String codigo);
    List<Producto> findByActivoTrue(Sort sort);
    List<Producto> findByActivoTrueAndCategoriaNombreIgnoreCase(String categoriaNombre, Sort sort);
    boolean existsByCategoriaId(Long categoriaId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.codigo = :codigo AND p.activo = true")
    Optional<Producto> bloquearPorCodigo(@Param("codigo") String codigo);

    // Para cancelaciones: hay que devolver stock incluso si el producto fue desactivado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.codigo = :codigo")
    Optional<Producto> bloquearIncluyendoInactivos(@Param("codigo") String codigo);
}
