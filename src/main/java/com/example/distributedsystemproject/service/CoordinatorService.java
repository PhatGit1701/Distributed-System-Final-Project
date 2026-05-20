package com.example.distributedsystemproject.service;

import com.example.distributedsystemproject.component.FailureDetectorComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CoordinatorService {

    @Autowired @Qualifier("jdbcNode1") private JdbcTemplate jdbcNode1;
    @Autowired @Qualifier("jdbcNode2") private JdbcTemplate jdbcNode2;
    @Autowired @Qualifier("jdbcNode3") private JdbcTemplate jdbcNode3;
    @Autowired private FailureDetectorComponent failureDetectorService;

    private JdbcTemplate getJdbcTemplate(String nodeId) {
        return switch (nodeId) {
            case "NODE_1" -> jdbcNode1;
            case "NODE_2" -> jdbcNode2;
            case "NODE_3" -> jdbcNode3;
            default -> throw new IllegalArgumentException("Unknown Node");
        };
    }

    // --- 1. WRITE-ALL-AVAILABLE ---
    public void writeStock(String sku, int quantity, String warehouseId) {
        String[] allNodes = {"NODE_1", "NODE_2", "NODE_3"};
        List<String> activeNodes = new ArrayList<>();
        List<String> downedNodes = new ArrayList<>();

        for (String node : allNodes) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                activeNodes.add(node);
            } else {
                downedNodes.add(node);
            }
        }

        if (activeNodes.isEmpty()) {
            throw new RuntimeException("Tất cả các node đều sập. Hệ thống ngừng ghi!");
        }

        for (String activeNode : activeNodes) {
            JdbcTemplate jdbc = getJdbcTemplate(activeNode);

            // Ghi dữ liệu thực tế vào Node đang sống
            String sqlWrite = "INSERT INTO stock_levels (sku, quantity, warehouse_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE quantity = ?";
            jdbc.update(sqlWrite, sku, quantity, warehouseId, quantity);

            // Ghi log HỘ các node đã sập (Decentralized Logging)
            for (String downedNode : downedNodes) {
                String sqlLog = "INSERT INTO recovery_log (target_node, sku, quantity) VALUES (?, ?, ?)";
                jdbc.update(sqlLog, downedNode, sku, quantity);
            }
        }
    }

    // --- 2. READ-ONE ---
    public Map<String, Object> readStock(String sku) {
        String[] allNodes = {"NODE_1", "NODE_2", "NODE_3"};
        for (String node : allNodes) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                try {
                    String sql = "SELECT * FROM stock_levels WHERE sku = ?";
                    Map<String, Object> result = getJdbcTemplate(node).queryForMap(sql, sku);
                    result.put("Served_By_Node", node);
                    return result;
                } catch (Exception e) {
                    // Node có thể vừa sập, thử node ACTIVE tiếp theo
                }
            }
        }
        throw new RuntimeException("Không có node nào ACTIVE để phục vụ lệnh đọc.");
    }

    // --- 3. RECOVERY (Catch-up) ---
    @Async
    public void processRecovery(String recoveringNodeId) {
        System.out.println("🔄 Bắt đầu Catch-up cho: " + recoveringNodeId);
        failureDetectorService.nodeStatusMap.put(recoveringNodeId, "RECOVERING");

        String helperNode = null;
        for (String node : new String[]{"NODE_1", "NODE_2", "NODE_3"}) {
            if ("ACTIVE".equals(failureDetectorService.nodeStatusMap.get(node))) {
                helperNode = node;
                break;
            }
        }

        if (helperNode == null) {
            System.err.println("Không có Node ACTIVE nào để lấy log phục hồi.");
            return;
        }

        JdbcTemplate helperJdbc = getJdbcTemplate(helperNode);
        JdbcTemplate targetJdbc = getJdbcTemplate(recoveringNodeId);

        // Đọc log từ Helper Node
        List<Map<String, Object>> logs = helperJdbc.queryForList(
                "SELECT * FROM recovery_log WHERE target_node = ?", recoveringNodeId);

        for (Map<String, Object> log : logs) {
            String sku = (String) log.get("sku");
            int quantity = (Integer) log.get("quantity");

            // Giả lập độ trễ mạng để có thời gian test Stale Read
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

            String sql = "INSERT INTO stock_levels (sku, quantity, warehouse_id) VALUES (?, ?, 'WH-RECOVER') ON DUPLICATE KEY UPDATE quantity = ?";
            targetJdbc.update(sql, sku, quantity, quantity);

            // Xóa log trên Helper Node sau khi copy xong
            helperJdbc.update("DELETE FROM recovery_log WHERE log_id = ?", log.get("log_id"));
        }

        failureDetectorService.nodeStatusMap.put(recoveringNodeId, "ACTIVE");
        System.out.println("✅ " + recoveringNodeId + " đã cập nhật xong và chuyển về ACTIVE!");
    }
}