package gaselvolcan.pedidos.service;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.exception.*;
import gaselvolcan.pedidos.model.*;
import gaselvolcan.pedidos.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductoService {
    private final ProductoRepository productos;
    private final CategoriaService categorias;
    public ProductoService(ProductoRepository productos, CategoriaService categorias) {
        this.productos = productos; this.categorias = categorias;
    }
    @Transactional(readOnly=true)
    public List<ProductoResponse> listar(String categoria) {
        Sort orden = Sort.by("codigo");
        List<Producto> lista = (categoria == null || categoria.isBlank())
            ? productos.findByActivoTrue(orden)
            : productos.findByActivoTrueAndCategoriaNombreIgnoreCase(categoria.trim(), orden);
        return lista.stream().map(this::respuesta).toList();
    }
    @Transactional(readOnly=true)
    public ProductoResponse buscar(String codigo) { return respuesta(buscarActivo(codigo)); }
    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (productos.findByCodigo(codigo).isPresent()) throw new ConflictoNegocioException("Código de producto ocupado: " + codigo);
        Producto p = new Producto();
        p.setCodigo(codigo);
        asignar(p, req);
        return respuesta(productos.save(p));
    }
    @Transactional
    public ProductoResponse actualizar(String codigo, ProductoRequest req) {
        Producto p = productos.findByCodigo(codigo.toUpperCase()).orElseThrow(() ->
            new RecursoNoEncontradoException("Producto no encontrado: " + codigo));
        if (!p.getCodigo().equalsIgnoreCase(req.codigo()))
            throw new IllegalArgumentException("El código del producto no se puede modificar");
        asignar(p, req);
        p.setActivo(true);
        return respuesta(p);
    }
    @Transactional
    public void eliminar(String codigo) {
        Producto p = buscarActivo(codigo);
        p.setActivo(false); // Borrado lógico: conserva el historial de las boletas
    }
    private Producto buscarActivo(String codigo) {
        return productos.findByCodigoAndActivoTrue(codigo.toUpperCase()).orElseThrow(() ->
            new RecursoNoEncontradoException("Producto no encontrado: " + codigo));
    }
    private void asignar(Producto p, ProductoRequest req) {
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setUnidad(req.unidad().trim());
        p.setPrecioResidencial(req.precioResidencial());
        p.setPrecioComercial(req.precioComercial());
        p.setStock(req.stock());
        p.setImagenUrl(req.imagenUrl());
        p.setCategoria(categorias.obtener(req.categoriaId()));
    }
    private ProductoResponse respuesta(Producto p) {
        return new ProductoResponse(p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getUnidad(),
            p.getPrecioResidencial(), p.getPrecioComercial(), p.getStock(), p.getImagenUrl(),
            p.isActivo(), p.getCategoria().getId(), p.getCategoria().getNombre());
    }
}
