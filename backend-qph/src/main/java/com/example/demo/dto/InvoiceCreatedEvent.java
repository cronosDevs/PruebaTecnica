package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceCreatedEvent {
    private String invoiceId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
