package gaselvolcan.pedidos.service;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.exception.*;
import gaselvolcan.pedidos.model.*;
import gaselvolcan.pedidos.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class BoletaService {
    private final BoletaRepository boletas;
    private final ProductoRepository productos;
    public BoletaService(BoletaRepository boletas, ProductoRepository productos) {
        this.boletas = boletas; this.productos = productos;
    }

    @Transactional
    public BoletaResponse crear(CrearBoletaRequest req, Authentication usuario) {
        if (usuario == null || usuario.getName() == null || usuario.getName().isBlank())
            throw new AccessDeniedException("Se requiere una sesión válida");
        // Agrupar códigos repetidos y ordenar: previene compras duplicadas en la misma boleta
        // y mantiene un orden de bloqueo consistente frente a pedidos simultáneos.
        SortedMap<String, Integer> unidades = new TreeMap<>();
        for (CrearBoletaRequest.ItemPedidoRequest item : req.detalles()) {
            String codigo = item.codigoProducto().trim().toUpperCase();
            if (codigo.isBlank()) throw new IllegalArgumentException("Código de producto vacío");
            unidades.merge(codigo, item.cantidad(), Math::addExact);
        }
        Boleta boleta = new Boleta();
        boleta.setClienteCorreo(usuario.getName().toLowerCase(Locale.ROOT));
        boleta.setNombreCliente(req.nombreCliente().trim());
        boleta.setDireccion(req.direccion().trim());
        boleta.setComuna(req.comuna().trim());
        boleta.setTipoPrecio(req.tipoPrecio());
        BigDecimal total = BigDecimal.ZERO;
        for (var item : unidades.entrySet()) {
            Producto p = productos.bloquearPorCodigo(item.getKey()).orElseThrow(() ->
                new RecursoNoEncontradoException("Producto no disponible: " + item.getKey()));
            int cantidad = item.getValue();
            if (p.getStock() < cantidad) throw new ConflictoNegocioException(
                "Stock insuficiente para " + p.getCodigo() + ". Disponible: " + p.getStock());
            BigDecimal precio = req.tipoPrecio() == TipoPrecio.COMERCIAL
                ? p.getPrecioComercial() : p.getPrecioResidencial();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
            p.setStock(p.getStock() - cantidad);
            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(p);
            detalle.setCodigoProducto(p.getCodigo());
            detalle.setNombreProducto(p.getNombre());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precio); // Foto del precio, no cambia con el catálogo
            detalle.setSubtotal(subtotal);
            boleta.agregarDetalle(detalle);
            total = total.add(subtotal);
        }
        boleta.setTotal(total);
        return respuesta(boletas.saveAndFlush(boleta));
    }

    @Transactional(readOnly=true)
    public List<BoletaResponse> listar(Authentication usuario) {
        List<Boleta> lista = esInterno(usuario)
            ? boletas.findAllByOrderByFechaDesc()
            : boletas.findByClienteCorreoIgnoreCaseOrderByFechaDesc(usuario.getName());
        return lista.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly=true)
    public BoletaResponse buscar(Long id, Authentication usuario) {
        Boleta boleta = boletas.findById(id).orElseThrow(() ->
            new RecursoNoEncontradoException("Boleta no encontrada: " + id));
        if (!esInterno(usuario) && !boleta.getClienteCorreo().equalsIgnoreCase(usuario.getName()))
            throw new AccessDeniedException("No puedes consultar boletas de otro cliente");
        return respuesta(boleta);
    }

    @Transactional
    public BoletaResponse cambiarEstado(Long id, EstadoBoletaRequest req) {
        Boleta boleta = boletas.bloquearPorId(id).orElseThrow(() ->
            new RecursoNoEncontradoException("Boleta no encontrada: " + id));
        EstadoBoleta actual = boleta.getEstado();
        EstadoBoleta nuevo = req.estado();
        if (actual == nuevo) return respuesta(boleta);
        boolean permitido = switch (actual) {
            case RECIBIDA -> nuevo == EstadoBoleta.EN_PREPARACION || nuevo == EstadoBoleta.CANCELADA;
            case EN_PREPARACION -> nuevo == EstadoBoleta.ENTREGADA || nuevo == EstadoBoleta.CANCELADA;
            case ENTREGADA, CANCELADA -> false;
        };
        if (!permitido) throw new ConflictoNegocioException("Transición no permitida: " + actual + " -> " + nuevo);
        if (nuevo == EstadoBoleta.CANCELADA) {
            // Conservar las líneas históricas; restaurar las unidades reservadas.
            // Boleta bloqueada evita doble reintegro. Bloquear productos en orden estable.
            List<DetalleBoleta> detalles = new ArrayList<>(boleta.getDetalles());
            detalles.sort(Comparator.comparing(DetalleBoleta::getCodigoProducto));
            for (DetalleBoleta d : detalles) {
                Producto p = productos.bloquearIncluyendoInactivos(d.getCodigoProducto()).orElseThrow(() ->
                    new ConflictoNegocioException("Producto desactivado al cancelar: " + d.getCodigoProducto()));
                p.setStock(Math.addExact(p.getStock(), d.getCantidad()));
            }
        }
        boleta.setEstado(nuevo);
        return respuesta(boleta);
    }

    private boolean esInterno(Authentication usuario) {
        return usuario.getAuthorities().stream().anyMatch(a ->
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDEDOR"));
    }
    private BoletaResponse respuesta(Boleta b) {
        var detalles = b.getDetalles().stream().map(d -> new BoletaResponse.DetalleResponse(
            d.getCodigoProducto(), d.getNombreProducto(), d.getCantidad(),
            d.getPrecioUnitario(), d.getSubtotal())).toList();
        return new BoletaResponse(b.getId(), b.getClienteCorreo(), b.getNombreCliente(), b.getDireccion(),
            b.getComuna(), b.getTipoPrecio(), b.getEstado(), b.getFecha(), b.getTotal(), detalles);
    }
}
