package com.example.distributedsystemproject.service;

import com.example.distributedsystemproject.component.FailureDetectorComponent;
import com.example.distributedsystemproject.model.ReadMode;
import com.example.distributedsystemproject.model.RecoveryLog;
import com.example.distributedsystemproject.model.StockLevel;
import com.example.distributedsystemproject.model.StockView;
import com.example.distributedsystemproject.repository.node1.Node1Repository;
import com.example.distributedsystemproject.repository.node2.Node2Repository;
import com.example.distributedsystemproject.repository.node3.Node3Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class CoordinatorService {

    @Autowired private Node1Repository node1Repository;
    @Autowired private Node2Repository node2Repository;
    @Autowired private Node3Repository node3Repository;
    @Autowired private FailureDetectorComponent failureDetectorService;

    @Value("${replication.quorum.read.full:2}")
    private int readQuorumFull;

    @Value("${replication.quorum.read.partial:2}")
    private int readQuorumPartial;

    private record NodeRead(String nodeId, StockLevel stock) {}

    private void upsert(String nodeId, String sku, int quantity, String warehouseId, LocalDateTime updatedAt, long version, String writeId) {
        switch (nodeId) {
            case "NODE_1" -> node1Repository.upsertStock(sku, quantity, warehouseId, updatedAt, version, writeId);
            case "NODE_2" -> node2Repository.upsertStock(sku, quantity, warehouseId, updatedAt, version, writeId);
            case "NODE_3" -> node3Repository.upsertStock(sku, quantity, warehouseId, updatedAt, version, writeId);
        }
    }

    private void insertLog(String nodeId, String targetNode, String sku, int quantity, String warehouseId, long version, String writeId) {
        switch (nodeId) {
            case "NODE_1" -> node1Repository.insertRecoveryLog(targetNode, sku, quantity, warehouseId, version, writeId);
            case "NODE_2" -> node2Repository.insertRecoveryLog(targetNode, sku, quantity, warehouseId, version, writeId);
            case "NODE_3" -> node3Repository.insertRecoveryLog(targetNode, sku, quantity, warehouseId, version, writeId);
        }
    }

    private boolean isNodeReachable(String nodeId) {
        String status = failureDetectorService.nodeStatusMap.get(nodeId);
        return "ACTIVE".equals(status) || "RECOVERING".equals(status);
    }

    private Optional<StockLevel> findStock(String nodeId, String sku) {
        if (!isNodeReachable(nodeId)) {
            return Optional.empty();
        }
        try {
            return switch (nodeId) {
                case "NODE_1" -> node1Repository.findStockBySku(sku);
                case "NODE_2" -> node2Repository.findStockBySku(sku);
                case "NODE_3" -> node3Repository.findStockBySku(sku);
                default -> Optional.empty();
            };
        } catch (Exception ex) {
            failureDetectorService.nodeStatusMap.put(nodeId, "DOWN");
            System.err.println("⚠️ Không đọc được stock_levels tại " + nodeId + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private List<RecoveryLog> findLogs(String nodeId, String targetNode) {
        return switch (nodeId) {
            case "NODE_1" -> node1Repository.findRecoveryLogs(targetNode);
            case "NODE_2" -> node2Repository.findRecoveryLogs(targetNode);
            case "NODE_3" -> node3Repository.findRecoveryLogs(targetNode);
            default -> List.of();
        };
    }

    private void deleteLog(String nodeId, Long logId) {
        switch (nodeId) {
            case "NODE_1" -> node1Repository.deleteRecoveryLog(logId);
            case "NODE_2" -> node2Repository.deleteRecoveryLog(logId);
            case "NODE_3" -> node3Repository.deleteRecoveryLog(logId);
        }
    }

    public List<String> getReplicaSet(String sku, String replicationMode) {
        if ("PARTIAL".equalsIgnoreCase(replicationMode)) {
            int hash = Math.abs(sku.hashCode());
            int subset = hash % 3;
            if (subset == 0) {
                return List.of("NODE_1", "NODE_2");
            } else if (subset == 1) {
                return List.of("NODE_2", "NODE_3");
            } else {
                return List.of("NODE_3", "NODE_1");
            }
        } else {
            return List.of("NODE_1", "NODE_2", "NODE_3");
        }
    }

    private int getReadQuorum(String replicationMode) {
        if ("PARTIAL".equalsIgnoreCase(replicationMode)) {
            return readQuorumPartial;
        }
        return readQuorumFull;
    }

    private long stockVersion(StockLevel stock) {
        return stock.getVersion() != null ? stock.getVersion() : 0L;
    }

    private long logVersion(RecoveryLog log) {
        return log.getVersion() != null ? log.getVersion() : 0L;
    }

    private long allocateNextVersion(String sku, List<String> replicaSet) {
        long max = 0L;
        for (String node : replicaSet) {
            if (!"ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                continue;
            }
            Optional<StockLevel> stock = findStock(node, sku);
            if (stock.isPresent()) {
                max = Math.max(max, stockVersion(stock.get()));
            }
        }
        return max + 1L;
    }

    private Comparator<StockLevel> versionComparator() {
        return Comparator
                .comparingLong(this::stockVersion)
                .thenComparing(StockLevel::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private List<String> activeNodesInSet(List<String> replicaSet) {
        List<String> active = new ArrayList<>();
        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                active.add(node);
            }
        }
        return active;
    }

    private List<NodeRead> readAllActiveWithData(List<String> replicaSet, String sku) {
        List<NodeRead> reads = new ArrayList<>();
        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                findStock(node, sku).ifPresent(stock -> reads.add(new NodeRead(node, stock)));
            }
        }
        return reads;
    }

    private List<String> nodesWithData(List<String> replicaSet, String sku) {
        List<String> actual = new ArrayList<>();
        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                if (findStock(node, sku).isPresent()) {
                    actual.add(node);
                }
            }
        }
        return actual;
    }

    private boolean isStale(StockLevel local, StockLevel winner) {
        return stockVersion(local) < stockVersion(winner);
    }

    private List<String> performReadRepair(List<String> replicaSet, StockLevel winner) {
        List<String> repaired = new ArrayList<>();
        LocalDateTime updatedAt = winner.getUpdatedAt() != null ? winner.getUpdatedAt() : LocalDateTime.now();
        long version = stockVersion(winner);
        String writeId = winner.getWriteId() != null ? winner.getWriteId() : UUID.randomUUID().toString();

        for (String node : replicaSet) {
            if (!"ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                continue;
            }
            Optional<StockLevel> local = findStock(node, winner.getSku());
            if (local.isEmpty() || isStale(local.get(), winner)) {
                try {
                    upsert(node, winner.getSku(), winner.getQuantity(), winner.getWarehouseId(), updatedAt, version, writeId);
                    repaired.add(node);
                    System.out.println("🔧 [Read Repair] Đồng bộ SKU " + winner.getSku() + " lên " + node + " (version " + version + ")");
                } catch (Exception ex) {
                    System.err.println("❌ Read repair thất bại tại " + node + ": " + ex.getMessage());
                }
            }
        }
        return repaired;
    }

    private StockView buildView(
            StockLevel winner,
            String servedByNode,
            String replicationMode,
            ReadMode readMode,
            List<String> replicaSet,
            List<String> actualNodes,
            int quorumRequired,
            boolean quorumMet,
            int replicasContacted,
            List<String> readRepairedNodes,
            boolean possiblyStale
    ) {
        return new StockView(
                winner.getSku(),
                winner.getQuantity(),
                winner.getWarehouseId(),
                servedByNode,
                replicationMode.toUpperCase(),
                readMode.name(),
                replicaSet,
                actualNodes,
                stockVersion(winner),
                winner.getWriteId(),
                winner.getUpdatedAt(),
                quorumRequired,
                quorumMet,
                replicasContacted,
                readRepairedNodes,
                possiblyStale
        );
    }

    public void writeStock(String sku, int quantity, String warehouseId) {
        writeStock(sku, quantity, warehouseId, "FULL");
    }

    public void writeStock(String sku, int quantity, String warehouseId, String replicationMode) {
        List<String> replicaSet = getReplicaSet(sku, replicationMode);
        List<String> activeNodes = new ArrayList<>();
        List<String> unavailableNodes = new ArrayList<>();

        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                activeNodes.add(node);
            } else {
                unavailableNodes.add(node);
            }
        }

        if (activeNodes.isEmpty()) {
            throw new RuntimeException("Tất cả các node trong replica set của SKU này đều sập. Hệ thống ngừng ghi!");
        }

        long version = allocateNextVersion(sku, replicaSet);
        String writeId = UUID.randomUUID().toString();
        LocalDateTime updatedAt = LocalDateTime.now();

        List<String> successfulNodes = new ArrayList<>();
        List<String> failedDuringWrite = new ArrayList<>();

        for (String activeNode : activeNodes) {
            try {
                upsert(activeNode, sku, quantity, warehouseId, updatedAt, version, writeId);
                successfulNodes.add(activeNode);
            } catch (Exception ex) {
                failedDuringWrite.add(activeNode);
                failureDetectorService.nodeStatusMap.put(activeNode, "DOWN");
                System.err.println("❌ Ghi thất bại tại " + activeNode + ". Node được đánh dấu DOWN và vẫn tiếp tục ghi node khác.");
            }
        }

        if (successfulNodes.isEmpty()) {
            throw new RuntimeException("Không ghi được vào node nào đang hoạt động.");
        }

        unavailableNodes.addAll(failedDuringWrite);

        for (String successfulNode : successfulNodes) {
            for (String unavailableNode : unavailableNodes) {
                if (!successfulNode.equals(unavailableNode)) {
                    insertLog(successfulNode, unavailableNode, sku, quantity, warehouseId, version, writeId);
                }
            }
        }
    }

    public StockView readStock(String sku) {
        return readStock(sku, "FULL", ReadMode.ONE);
    }

    public StockView readStock(String sku, String replicationMode) {
        return readStock(sku, replicationMode, ReadMode.ONE);
    }

    public StockView readStock(String sku, String replicationMode, ReadMode readMode) {
        List<String> replicaSet = getReplicaSet(sku, replicationMode);
        List<String> activeInSet = activeNodesInSet(replicaSet);

        if (activeInSet.isEmpty()) {
            throw new RuntimeException("Không còn server nào hoạt động");
        }

        List<String> actualNodes = nodesWithData(replicaSet, sku);
        int quorumRequired = getReadQuorum(replicationMode);
        int replicasContacted = activeInSet.size();

        return switch (readMode) {
            case ONE -> readOne(sku, replicationMode, replicaSet, actualNodes, quorumRequired, replicasContacted);
            case LATEST -> readLatest(sku, replicationMode, replicaSet, actualNodes, quorumRequired, replicasContacted);
            case QUORUM -> readQuorum(sku, replicationMode, replicaSet, actualNodes, quorumRequired, replicasContacted);
        };
    }

    private StockView readOne(String sku, String replicationMode, List<String> replicaSet,
                              List<String> actualNodes, int quorumRequired, int replicasContacted) {
        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                Optional<StockLevel> stock = findStock(node, sku);
                if (stock.isPresent()) {
                    return buildView(
                            stock.get(), node, replicationMode, ReadMode.ONE,
                            replicaSet, actualNodes, quorumRequired, true,
                            replicasContacted, List.of(), true
                    );
                }
            }
        }
        throw new RuntimeException("Dữ liệu không tồn tại trong database");
    }

    private StockView readLatest(String sku, String replicationMode, List<String> replicaSet,
                               List<String> actualNodes, int quorumRequired, int replicasContacted) {
        List<NodeRead> reads = readAllActiveWithData(replicaSet, sku);
        if (reads.isEmpty()) {
            throw new RuntimeException("Dữ liệu không tồn tại trong database");
        }

        NodeRead winnerRead = reads.stream()
                .max(Comparator.comparing(NodeRead::stock, versionComparator()))
                .orElseThrow();

        List<String> repaired = performReadRepair(replicaSet, winnerRead.stock());

        return buildView(
                winnerRead.stock(), winnerRead.nodeId(), replicationMode, ReadMode.LATEST,
                replicaSet, actualNodes, quorumRequired, true,
                replicasContacted, repaired, false
        );
    }

    private StockView readQuorum(String sku, String replicationMode, List<String> replicaSet,
                                 List<String> actualNodes, int quorumRequired, int replicasContacted) {
        List<NodeRead> reads = readAllActiveWithData(replicaSet, sku);
        if (reads.isEmpty()) {
            throw new RuntimeException("Dữ liệu không tồn tại trong database");
        }

        Map<Long, List<NodeRead>> byVersion = reads.stream()
                .collect(Collectors.groupingBy(nr -> stockVersion(nr.stock()), LinkedHashMap::new, Collectors.toList()));

        Optional<Map.Entry<Long, List<NodeRead>>> quorumEntry = byVersion.entrySet().stream()
                .filter(e -> e.getValue().size() >= quorumRequired)
                .max(Map.Entry.comparingByKey());

        if (quorumEntry.isEmpty()) {
            throw new RuntimeException("Quorum không đạt: cần ít nhất " + quorumRequired
                    + " replica cùng version, nhưng chỉ có " + reads.size() + " bản ghi phân tán.");
        }

        List<NodeRead> quorumReads = quorumEntry.get().getValue();
        NodeRead winnerRead = quorumReads.stream()
                .max(Comparator.comparing((NodeRead nr) -> nr.stock().getUpdatedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(quorumReads.get(0));

        List<String> repaired = performReadRepair(replicaSet, winnerRead.stock());

        return buildView(
                winnerRead.stock(), winnerRead.nodeId(), replicationMode, ReadMode.QUORUM,
                replicaSet, actualNodes, quorumRequired, true,
                replicasContacted, repaired, false
        );
    }

    private boolean shouldApplyLog(StockLevel existing, RecoveryLog log) {
        long existingVersion = stockVersion(existing);
        long incomingVersion = logVersion(log);
        if (incomingVersion > existingVersion) {
            return true;
        }
        if (incomingVersion < existingVersion) {
            return false;
        }
        if (existing.getUpdatedAt() != null && log.getCreatedAt() != null) {
            return log.getCreatedAt().isAfter(existing.getUpdatedAt());
        }
        return true;
    }

    @Async
    public void processRecovery(String recoveringNodeId) {
        failureDetectorService.nodeStatusMap.put(recoveringNodeId, "RECOVERING");
        String[] allNodes = {"NODE_1", "NODE_2", "NODE_3"};

        for (String helperNode : allNodes) {
            if (helperNode.equals(recoveringNodeId)) continue;

            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(helperNode))) {
                List<RecoveryLog> logs = findLogs(helperNode, recoveringNodeId);
                for (RecoveryLog log : logs) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

                    Optional<StockLevel> existingStockOpt = findStock(recoveringNodeId, log.getSku());
                    boolean shouldUpdate = existingStockOpt.isEmpty()
                            || shouldApplyLog(existingStockOpt.get(), log);

                    if (shouldUpdate) {
                        long version = logVersion(log);
                        String writeId = log.getWriteId() != null ? log.getWriteId() : UUID.randomUUID().toString();
                        LocalDateTime updatedAt = log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now();
                        upsert(recoveringNodeId, log.getSku(), log.getQuantity(),
                                log.getWarehouseId() != null ? log.getWarehouseId() : "WH-MAIN",
                                updatedAt, version, writeId);
                        System.out.println("🔄 Đã khôi phục SKU " + log.getSku() + " trên " + recoveringNodeId + " từ log của " + helperNode);
                    } else {
                        System.out.println("⏭️ Bỏ qua SKU " + log.getSku() + " trên " + recoveringNodeId + " (version hiện tại >= log)");
                    }
                    deleteLog(helperNode, log.getLogId());
                }
            }
        }

        List<RecoveryLog> ownLogs = getNodeRecoveryLogs(recoveringNodeId);
        for (RecoveryLog log : ownLogs) {
            String targetNode = log.getTargetNode();
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(targetNode))) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

                Optional<StockLevel> targetStockOpt = findStock(targetNode, log.getSku());
                boolean shouldUpdate = targetStockOpt.isEmpty()
                        || shouldApplyLog(targetStockOpt.get(), log);

                if (shouldUpdate) {
                    long version = logVersion(log);
                    String writeId = log.getWriteId() != null ? log.getWriteId() : UUID.randomUUID().toString();
                    LocalDateTime updatedAt = log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now();
                    upsert(targetNode, log.getSku(), log.getQuantity(),
                            log.getWarehouseId() != null ? log.getWarehouseId() : "WH-MAIN",
                            updatedAt, version, writeId);
                    System.out.println("🚀 Đã đẩy khôi phục SKU " + log.getSku() + " từ " + recoveringNodeId + " sang target " + targetNode);
                } else {
                    System.out.println("⏭️ Bỏ qua đẩy phục hồi SKU " + log.getSku() + " sang " + targetNode + " (version target >= log)");
                }
                deleteLog(recoveringNodeId, log.getLogId());
            }
        }

        failureDetectorService.nodeStatusMap.put(recoveringNodeId, "ACTIVE");
    }

    public List<StockLevel> getNodeStocks(String nodeId) {
        try {
            return switch (nodeId) {
                case "NODE_1" -> node1Repository.findAll();
                case "NODE_2" -> node2Repository.findAll();
                case "NODE_3" -> node3Repository.findAll();
                default -> List.of();
            };
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<RecoveryLog> getNodeRecoveryLogs(String nodeId) {
        try {
            return switch (nodeId) {
                case "NODE_1" -> node1Repository.findAllRecoveryLogs();
                case "NODE_2" -> node2Repository.findAllRecoveryLogs();
                case "NODE_3" -> node3Repository.findAllRecoveryLogs();
                default -> List.of();
            };
        } catch (Exception e) {
            return List.of();
        }
    }

    public void setNodeOffline(String nodeId) {
        switch (nodeId) {
            case "NODE_1" -> node1Repository.setOffline();
            case "NODE_2" -> node2Repository.setOffline();
            case "NODE_3" -> node3Repository.setOffline();
        }
        failureDetectorService.nodeStatusMap.put(nodeId, "DOWN");
    }

    public void setNodeOnline(String nodeId) {
        switch (nodeId) {
            case "NODE_1" -> node1Repository.setOnline();
            case "NODE_2" -> node2Repository.setOnline();
            case "NODE_3" -> node3Repository.setOnline();
        }
    }

    @Scheduled(fixedRate = 10000)
    public void backgroundSyncLogs() {
        String[] allNodes = {"NODE_1", "NODE_2", "NODE_3"};

        for (String sourceNode : allNodes) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(sourceNode))) {
                List<RecoveryLog> logs = getNodeRecoveryLogs(sourceNode);
                for (RecoveryLog log : logs) {
                    String targetNode = log.getTargetNode();
                    if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(targetNode))) {
                        try {
                            Optional<StockLevel> targetStockOpt = findStock(targetNode, log.getSku());
                            boolean shouldUpdate = targetStockOpt.isEmpty()
                                    || shouldApplyLog(targetStockOpt.get(), log);

                            if (shouldUpdate) {
                                long version = logVersion(log);
                                String writeId = log.getWriteId() != null ? log.getWriteId() : UUID.randomUUID().toString();
                                LocalDateTime updatedAt = log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now();
                                upsert(targetNode, log.getSku(), log.getQuantity(),
                                        log.getWarehouseId() != null ? log.getWarehouseId() : "WH-MAIN",
                                        updatedAt, version, writeId);
                                System.out.println("🔄 [Background Sync] Đã đồng bộ SKU " + log.getSku() + " từ " + sourceNode + " sang " + targetNode);
                            }
                            deleteLog(sourceNode, log.getLogId());
                        } catch (Exception e) {
                            System.err.println("❌ Lỗi khi đồng bộ log nền: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
