package com.example.distributedsystemproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_log")
public class RecoveryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "target_node", nullable = false)
    private String targetNode;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId = "WH-MAIN";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "version")
    private Long version;

    @Column(name = "write_id")
    private String writeId;

    public Long getLogId() { return logId; }
    public String getEventId() { return eventId; }
    public String getTargetNode() { return targetNode; }
    public String getSku() { return sku; }
    public Integer getQuantity() { return quantity; }
    public String getWarehouseId() { return warehouseId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public String getWriteId() { return writeId; }
}