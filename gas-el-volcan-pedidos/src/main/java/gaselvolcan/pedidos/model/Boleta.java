package gaselvolcan.pedidos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boletas", indexes = @Index(name="idx_boleta_correo", columnList="cliente_correo"))
public class Boleta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="cliente_correo", nullable=false, length=200)
    private String clienteCorreo;
    @Column(nullable=false, length=150)
    private String nombreCliente;
    @Column(nullable=false, length=300)
    private String direccion;
    @Column(nullable=false, length=100)
    private String comuna;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private TipoPrecio tipoPrecio;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=25)
    private EstadoBoleta estado = EstadoBoleta.RECIBIDA;
    @Column(nullable=false, precision=14, scale=0)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable=false, updatable=false)
    private LocalDateTime fecha = LocalDateTime.now();
    @OneToMany(mappedBy="boleta", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<DetalleBoleta> detalles = new ArrayList<>();

    public Long getId() { return id; }
    public String getClienteCorreo() { return clienteCorreo; }
    public void setClienteCorreo(String clienteCorreo) { this.clienteCorreo = clienteCorreo; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }
    public TipoPrecio getTipoPrecio() { return tipoPrecio; }
    public void setTipoPrecio(TipoPrecio tipoPrecio) { this.tipoPrecio = tipoPrecio; }
    public EstadoBoleta getEstado() { return estado; }
    public void setEstado(EstadoBoleta estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDateTime getFecha() { return fecha; }
    public List<DetalleBoleta> getDetalles() { return detalles; }
    public void agregarDetalle(DetalleBoleta detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }
}
