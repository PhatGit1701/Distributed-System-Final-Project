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

    public void writeStock(String sku, int quantity, String warehouseId) {
        String[] allNodes = {"NODE_1", "NODE_2", "NODE_3"};
        List<String> activeNodes = new ArrayList<>();
        List<String> unavailableNodes = new ArrayList<>();

        for (String node : allNodes) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                activeNodes.add(node);
            } else {
                unavailableNodes.add(node);
            }
        }

        if (activeNodes.isEmpty()) {
            throw new RuntimeException("Tất cả các node đều sập. Hệ thống ngừng ghi!");
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
        for (String node : new String[]{"NODE_1", "NODE_2", "NODE_3"}) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                Optional<StockLevel> stock = findStock(node, sku);
                if (stock.isPresent()) return new StockView(stock.get().getSku(), stock.get().getQuantity(), stock.get().getWarehouseId(), node);
            }
        }
        throw new RuntimeException("Không có node nào ACTIVE để phục vụ lệnh đọc.");
    }

    @Async
    public void processRecovery(String recoveringNodeId) {
        failureDetectorService.nodeStatusMap.put(recoveringNodeId, "RECOVERING");
        String helperNode = null;
        for (String node : new String[]{"NODE_1", "NODE_2", "NODE_3"}) if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) { helperNode = node; break; }
        if (helperNode == null) return;

        for (RecoveryLog log : findLogs(helperNode, recoveringNodeId)) {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            upsert(recoveringNodeId, log.getSku(), log.getQuantity(), "WH-RECOVER");
            deleteLog(helperNode, log.getLogId());
        }
        failureDetectorService.nodeStatusMap.put(recoveringNodeId, "ACTIVE");
    }
}