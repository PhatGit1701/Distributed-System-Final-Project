package com.example.distributedsystemproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "recovery_log")
public class RecoveryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "target_node")
    private String targetNode;

    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity")
    private Integer quantity;

    public Long getLogId() { return logId; }
    public String getTargetNode() { return targetNode; }
    public String getSku() { return sku; }
    public Integer getQuantity() { return quantity; }
}