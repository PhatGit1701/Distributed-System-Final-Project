package com.example.distributedsystemproject.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

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

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public String getSku() { return sku; }
    public Integer getQuantity() { return quantity; }
    public String getWarehouseId() { return warehouseId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}