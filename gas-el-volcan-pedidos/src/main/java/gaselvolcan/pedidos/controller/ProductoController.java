package gaselvolcan.pedidos.controller;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name="Productos")
public class ProductoController {
    private final ProductoService servicio;
    public ProductoController(ProductoService servicio) { this.servicio = servicio; }
    @GetMapping
    @Operation(summary="Listar productos activos; filtrar opcionalmente por nombre de categoría")
    public List<ProductoResponse> listar(@RequestParam(required=false) String categoria) {
        return servicio.listar(categoria);
    }
    @GetMapping("/{codigo}")
    public ProductoResponse buscar(@PathVariable String codigo) { return servicio.buscar(codigo); }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req));
    }
    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(@PathVariable String codigo, @Valid @RequestBody ProductoRequest req) {
        return servicio.actualizar(codigo, req);
    }
    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Desactivar producto, sin borrar el historial")
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        servicio.eliminar(codigo); return ResponseEntity.noContent().build();
    }
}
