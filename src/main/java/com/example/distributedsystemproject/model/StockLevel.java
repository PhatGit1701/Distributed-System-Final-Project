package com.example.distributedsystemproject.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_levels")
public class StockLevel {
    @Id
    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "warehouse_id")
    private String warehouseId;

    public String getSku() { return sku; }
    public Integer getQuantity() { return quantity; }
    public String getWarehouseId() { return warehouseId; }
}