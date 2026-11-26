package gaselvolcan.pedidos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="detalles_boleta")
public class DetalleBoleta {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="boleta_id", nullable=false)
    private Boleta boleta;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="producto_id", nullable=false)
    private Producto producto;
    @Column(nullable=false, length=20)
    private String codigoProducto;
    @Column(nullable=false, length=230)
    private String nombreProducto;
    @Column(nullable=false)
    private Integer cantidad;
    @Column(nullable=false, precision=12, scale=0)
    private BigDecimal precioUnitario;
    @Column(nullable=false, precision=14, scale=0)
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public Boleta getBoleta() { return boleta; }
    public void setBoleta(Boleta boleta) { this.boleta = boleta; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
