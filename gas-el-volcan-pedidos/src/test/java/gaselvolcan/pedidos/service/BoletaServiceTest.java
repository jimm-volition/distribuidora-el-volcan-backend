package gaselvolcan.pedidos.service;

import gaselvolcan.pedidos.dto.*;
import gaselvolcan.pedidos.exception.ConflictoNegocioException;
import gaselvolcan.pedidos.model.*;
import gaselvolcan.pedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {
    @Mock BoletaRepository boletas;
    @Mock ProductoRepository productos;
    @InjectMocks BoletaService servicio;

    private Producto producto(int stock) {
        Producto p = new Producto();
        p.setCodigo("CL002"); p.setNombre("Cilindro GLP 11 kg");
        p.setPrecioResidencial(new BigDecimal("12000"));
        p.setPrecioComercial(new BigDecimal("11000"));
        p.setStock(stock);
        return p;
    }
    private CrearBoletaRequest pedido(TipoPrecio tipo, int cantidad) {
        return new CrearBoletaRequest("Ana", "Las Rosas 123", "Chillán", tipo,
            List.of(new CrearBoletaRequest.ItemPedidoRequest("CL002", cantidad)));
    }
    private UsernamePasswordAuthenticationToken cliente() {
        return new UsernamePasswordAuthenticationToken("ana@example.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }
    @Test
    void calculaPrecioResidencialYDescuentaStock() {
        Producto p = producto(10);
        when(productos.bloquearPorCodigo("CL002")).thenReturn(Optional.of(p));
        when(boletas.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = servicio.crear(pedido(TipoPrecio.RESIDENCIAL, 2), cliente());
        assertEquals(new BigDecimal("24000"), result.total());
        assertEquals(8, p.getStock());
        assertEquals("ana@example.com", result.clienteCorreo());
        assertEquals(new BigDecimal("12000"), result.detalles().get(0).precioUnitario());
    }
    @Test
    void usaPrecioComercialDelExcel() {
        Producto p = producto(10);
        when(productos.bloquearPorCodigo("CL002")).thenReturn(Optional.of(p));
        when(boletas.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = servicio.crear(pedido(TipoPrecio.COMERCIAL, 2), cliente());
        assertEquals(new BigDecimal("22000"), result.total());
    }
    @Test
    void rechazaStockInsuficiente() {
        Producto p = producto(1);
        when(productos.bloquearPorCodigo("CL002")).thenReturn(Optional.of(p));
        assertThrows(ConflictoNegocioException.class,
            () -> servicio.crear(pedido(TipoPrecio.RESIDENCIAL, 2), cliente()));
        assertEquals(1, p.getStock());
        verify(boletas, never()).saveAndFlush(any());
    }
    @Test
    void alCancelarReintegraStockYNoBorraHistorial() {
        Producto p = producto(8);
        p.setActivo(false); // Producto desactivado después de la venta
        Boleta b = new Boleta();
        b.setClienteCorreo("ana@example.com");
        b.setEstado(EstadoBoleta.RECIBIDA);
        DetalleBoleta detalle = new DetalleBoleta();
        detalle.setProducto(p);
        detalle.setCodigoProducto("CL002");
        detalle.setNombreProducto("Cilindro GLP 11 kg");
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("12000"));
        detalle.setSubtotal(new BigDecimal("24000"));
        b.agregarDetalle(detalle);
        when(boletas.bloquearPorId(1L)).thenReturn(Optional.of(b));
        when(productos.bloquearIncluyendoInactivos("CL002")).thenReturn(Optional.of(p));
        var result = servicio.cambiarEstado(1L, new EstadoBoletaRequest(EstadoBoleta.CANCELADA));
        assertEquals(EstadoBoleta.CANCELADA, result.estado());
        assertEquals(10, p.getStock());
        assertEquals(1, result.detalles().size());
    }

    @Test
    void bloqueaAccesoABoletaAjena() {
        Boleta ajena = new Boleta();
        ajena.setClienteCorreo("otra@example.com");
        when(boletas.findById(5L)).thenReturn(Optional.of(ajena));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> servicio.buscar(5L, cliente()));
    }
}
