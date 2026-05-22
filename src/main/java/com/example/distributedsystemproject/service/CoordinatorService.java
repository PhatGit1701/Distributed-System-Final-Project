package com.example.distributedsystemproject.service;

import com.example.distributedsystemproject.component.FailureDetectorComponent;
import com.example.distributedsystemproject.model.RecoveryLog;
import com.example.distributedsystemproject.model.StockLevel;
import com.example.distributedsystemproject.model.StockView;
import com.example.distributedsystemproject.repository.node1.Node1Repository;
import com.example.distributedsystemproject.repository.node2.Node2Repository;
import com.example.distributedsystemproject.repository.node3.Node3Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CoordinatorService {

    @Autowired private Node1Repository node1Repository;
    @Autowired private Node2Repository node2Repository;
    @Autowired private Node3Repository node3Repository;
    @Autowired private FailureDetectorComponent failureDetectorService;

    private void upsert(String nodeId, String sku, int quantity, String warehouseId) {
        switch (nodeId) { case "NODE_1" -> node1Repository.upsertStock(sku, quantity, warehouseId); case "NODE_2" -> node2Repository.upsertStock(sku, quantity, warehouseId); case "NODE_3" -> node3Repository.upsertStock(sku, quantity, warehouseId); }
    }

    private void insertLog(String nodeId, String targetNode, String sku, int quantity) {
        switch (nodeId) { case "NODE_1" -> node1Repository.insertRecoveryLog(targetNode, sku, quantity); case "NODE_2" -> node2Repository.insertRecoveryLog(targetNode, sku, quantity); case "NODE_3" -> node3Repository.insertRecoveryLog(targetNode, sku, quantity); }
    }

    private Optional<StockLevel> findStock(String nodeId, String sku) {
        return switch (nodeId) { case "NODE_1" -> node1Repository.findStockBySku(sku); case "NODE_2" -> node2Repository.findStockBySku(sku); case "NODE_3" -> node3Repository.findStockBySku(sku); default -> Optional.empty();};
    }

    private List<RecoveryLog> findLogs(String nodeId, String targetNode) {
        return switch (nodeId) { case "NODE_1" -> node1Repository.findRecoveryLogs(targetNode); case "NODE_2" -> node2Repository.findRecoveryLogs(targetNode); case "NODE_3" -> node3Repository.findRecoveryLogs(targetNode); default -> List.of();};
    }

    private void deleteLog(String nodeId, Long logId) {
        switch (nodeId) { case "NODE_1" -> node1Repository.deleteRecoveryLog(logId); case "NODE_2" -> node2Repository.deleteRecoveryLog(logId); case "NODE_3" -> node3Repository.deleteRecoveryLog(logId); }
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

        List<String> successfulNodes = new ArrayList<>();
        List<String> failedDuringWrite = new ArrayList<>();

        for (String activeNode : activeNodes) {
            try {
                upsert(activeNode, sku, quantity, warehouseId);
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
                    insertLog(successfulNode, unavailableNode, sku, quantity);
                }
            }
        }
    }

    public StockView readStock(String sku) {
        return readStock(sku, "FULL");
    }

    public StockView readStock(String sku, String replicationMode) {
        List<String> replicaSet = getReplicaSet(sku, replicationMode);
        List<String> actualNodes = new ArrayList<>();

        // Tìm các node thực tế đang có SKU này (và node đó đang ACTIVE)
        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                Optional<StockLevel> stock = findStock(node, sku);
                if (stock.isPresent()) {
                    actualNodes.add(node);
                }
            }
        }

        for (String node : replicaSet) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                Optional<StockLevel> stock = findStock(node, sku);
                if (stock.isPresent()) {
                    return new StockView(
                        stock.get().getSku(),
                        stock.get().getQuantity(),
                        stock.get().getWarehouseId(),
                        node,
                        replicationMode.toUpperCase(),
                        replicaSet,
                        actualNodes
                    );
                }
            }
        }
        throw new RuntimeException("Không có node nào ACTIVE trong replica set để phục vụ lệnh đọc hoặc SKU chưa được khởi tạo.");
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
                    upsert(recoveringNodeId, log.getSku(), log.getQuantity(), "WH-RECOVER");
                    deleteLog(helperNode, log.getLogId());
                }
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
}