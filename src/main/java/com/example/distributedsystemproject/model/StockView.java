package com.example.distributedsystemproject.model;

import java.util.List;

public class StockView {
    private final String sku;
    private final int quantity;
    private final String warehouseId;
    private final String servedByNode;
    private final String replicationMode;
    private final List<String> replicaSet;
    private final List<String> actualNodes;

    public StockView(String sku, int quantity, String warehouseId, String servedByNode, String replicationMode, List<String> replicaSet, List<String> actualNodes) {
        this.sku = sku;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.servedByNode = servedByNode;
        this.replicationMode = replicationMode;
        this.replicaSet = replicaSet;
        this.actualNodes = actualNodes;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getWarehouseId() { return warehouseId; }
    public String getServedByNode() { return servedByNode; }
    public String getReplicationMode() { return replicationMode; }
    public List<String> getReplicaSet() { return replicaSet; }
    public List<String> getActualNodes() { return actualNodes; }
}