package gaselvolcan.pedidos.controller;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name="Categorías")
public class CategoriaController {
    private final CategoriaService servicio;
    public CategoriaController(CategoriaService servicio) { this.servicio = servicio; }
    @GetMapping
    @Operation(summary="Listar categorías")
    public List<CategoriaResponse> listar() { return servicio.listar(); }
    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) { return servicio.buscar(id); }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return servicio.actualizar(id, req);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id); return ResponseEntity.noContent().build();
    }
}
