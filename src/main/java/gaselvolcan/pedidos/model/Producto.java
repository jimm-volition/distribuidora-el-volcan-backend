package gaselvolcan.pedidos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos", indexes = @Index(name = "idx_producto_categoria", columnList = "categoria_id"))
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;
    @Column(nullable = false, length = 230)
    private String nombre;
    @Column(length = 1500)
    private String descripcion;
    @Column(nullable = false, length = 30)
    private String unidad;
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal precioResidencial;
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal precioComercial;
    @Column(nullable = false)
    private Integer stock;
    @Column(length = 750)
    private String imagenUrl;
    @Column(nullable = false)
    private boolean activo = true;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getPrecioResidencial() { return precioResidencial; }
    public void setPrecioResidencial(BigDecimal precioResidencial) { this.precioResidencial = precioResidencial; }
    public BigDecimal getPrecioComercial() { return precioComercial; }
    public void setPrecioComercial(BigDecimal precioComercial) { this.precioComercial = precioComercial; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}
