package gaselvolcan.pedidos.service;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.exception.*;
import gaselvolcan.pedidos.model.Categoria;
import gaselvolcan.pedidos.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias; this.productos = productos;
    }
    @Transactional(readOnly=true)
    public List<CategoriaResponse> listar() {
        return categorias.findAll(Sort.by("nombre")).stream().map(this::respuesta).toList();
    }
    @Transactional(readOnly=true)
    public CategoriaResponse buscar(Long id) { return respuesta(obtener(id)); }
    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categorias.existsByNombreIgnoreCase(nombre)) throw new ConflictoNegocioException("La categoría ya existe");
        return respuesta(categorias.save(new Categoria(nombre)));
    }
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria cat = obtener(id);
        String nuevo = req.nombre().trim();
        categorias.findByNombreIgnoreCase(nuevo).ifPresent(otra -> {
            if (!otra.getId().equals(id)) throw new ConflictoNegocioException("La categoría ya existe");
        });
        cat.setNombre(nuevo);
        return respuesta(cat);
    }
    @Transactional
    public void eliminar(Long id) {
        Categoria cat = obtener(id);
        if (productos.existsByCategoriaId(id)) throw new ConflictoNegocioException("La categoría tiene productos asociados");
        categorias.delete(cat);
    }
    public Categoria obtener(Long id) { return categorias.findById(id).orElseThrow(() ->
        new RecursoNoEncontradoException("Categoría no encontrada: " + id)); }
    private CategoriaResponse respuesta(Categoria c) { return new CategoriaResponse(c.getId(), c.getNombre()); }
}
