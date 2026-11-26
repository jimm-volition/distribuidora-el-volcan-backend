package gaselvolcan.pedidos.controller;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name="Boletas / pedidos")
public class BoletaController {
    private final BoletaService servicio;
    public BoletaController(BoletaService servicio) { this.servicio = servicio; }
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','CLIENTE')")
    @Operation(summary="ADMIN/VENDEDOR ven todas; CLIENTE solo las suyas")
    public List<BoletaResponse> listar(Authentication usuario) { return servicio.listar(usuario); }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','CLIENTE')")
    public BoletaResponse buscar(@PathVariable Long id, Authentication usuario) {
        return servicio.buscar(id, usuario);
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    @Operation(summary="Crear pedido, calcula precios y descuenta stock en una transacción")
    public ResponseEntity<BoletaResponse> crear(@Valid @RequestBody CrearBoletaRequest req, Authentication usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req, usuario));
    }
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoBoletaRequest req) {
        return servicio.cambiarEstado(id, req);
    }
}
