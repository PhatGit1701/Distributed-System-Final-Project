package com.example.distributedsystemproject.model;

import java.time.LocalDateTime;
import java.util.List;

public class StockView {
    private final String sku;
    private final int quantity;
    private final String warehouseId;
    private final String servedByNode;
    private final String replicationMode;
    private final String readMode;
    private final List<String> replicaSet;
    private final List<String> actualNodes;
    private final Long version;
    private final String writeId;
    private final LocalDateTime updatedAt;
    private final int quorumRequired;
    private final boolean quorumMet;
    private final int replicasContacted;
    private final List<String> readRepairedNodes;
    private final boolean possiblyStale;

    public StockView(
            String sku,
            int quantity,
            String warehouseId,
            String servedByNode,
            String replicationMode,
            String readMode,
            List<String> replicaSet,
            List<String> actualNodes,
            Long version,
            String writeId,
            LocalDateTime updatedAt,
            int quorumRequired,
            boolean quorumMet,
            int replicasContacted,
            List<String> readRepairedNodes,
            boolean possiblyStale
    ) {
        this.sku = sku;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.servedByNode = servedByNode;
        this.replicationMode = replicationMode;
        this.readMode = readMode;
        this.replicaSet = replicaSet;
        this.actualNodes = actualNodes;
        this.version = version;
        this.writeId = writeId;
        this.updatedAt = updatedAt;
        this.quorumRequired = quorumRequired;
        this.quorumMet = quorumMet;
        this.replicasContacted = replicasContacted;
        this.readRepairedNodes = readRepairedNodes;
        this.possiblyStale = possiblyStale;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getWarehouseId() { return warehouseId; }
    public String getServedByNode() { return servedByNode; }
    public String getReplicationMode() { return replicationMode; }
    public String getReadMode() { return readMode; }
    public List<String> getReplicaSet() { return replicaSet; }
    public List<String> getActualNodes() { return actualNodes; }
    public Long getVersion() { return version; }
    public String getWriteId() { return writeId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getQuorumRequired() { return quorumRequired; }
    public boolean isQuorumMet() { return quorumMet; }
    public int getReplicasContacted() { return replicasContacted; }
    public List<String> getReadRepairedNodes() { return readRepairedNodes; }
    public boolean isPossiblyStale() { return possiblyStale; }
}
