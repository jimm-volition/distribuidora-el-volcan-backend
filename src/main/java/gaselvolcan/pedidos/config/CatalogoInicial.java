package gaselvolcan.pedidos.config;

import gaselvolcan.pedidos.model.*;
import gaselvolcan.pedidos.repository.*;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Component
public class CatalogoInicial implements CommandLineRunner {
    private final JsonMapper mapper;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final TransactionTemplate transaccion;
    public CatalogoInicial(JsonMapper mapper, CategoriaRepository categorias,
                          ProductoRepository productos, TransactionTemplate transaccion) {
        this.mapper=mapper; this.categorias=categorias; this.productos=productos; this.transaccion=transaccion;
    }
    @Override
    public void run(String... args) throws Exception {
        try (InputStream archivo = new ClassPathResource("seed/productos.json").getInputStream()) {
            CatalogoProducto[] registros = mapper.readValue(archivo, CatalogoProducto[].class);
            transaccion.executeWithoutResult(status -> {
                for (CatalogoProducto r : registros) {
                    if (productos.findByCodigo(r.codigo()).isPresent()) continue;
                    Categoria categoria = categorias.findByNombreIgnoreCase(r.categoria()).orElseGet(() ->
                        categorias.save(new Categoria(r.categoria())));
                    Producto p = new Producto();
                    p.setCodigo(r.codigo()); p.setNombre(r.nombre()); p.setDescripcion(r.descripcion());
                    p.setUnidad(r.unidad()); p.setPrecioResidencial(r.precioResidencial());
                    p.setPrecioComercial(r.precioComercial()); p.setStock(r.stock());
                    p.setCategoria(categoria); p.setActivo(true);
                    productos.save(p);
                }
            });
        }
    }
    // Los campos coinciden con las cabeceras del XLSX, traducidas a camelCase.
    public record CatalogoProducto(String codigo, String categoria, String nombre,
       String descripcion, String unidad, BigDecimal precioResidencial,
       BigDecimal precioComercial, Integer stock) {}
}
