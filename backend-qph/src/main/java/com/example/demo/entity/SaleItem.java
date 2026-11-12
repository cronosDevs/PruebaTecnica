package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"venta_id", "producto_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"venta", "producto"})
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Sale venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Product producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 38, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal subtotal;

    public static SaleItem of(Product producto, int cantidad) {
        return SaleItem.builder()
                .producto(producto)
                .cantidad(cantidad)
                .precioUnitario(producto.getPrecio())
                .subtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)))
                .build();
    }
}
