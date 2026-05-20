package com.example.distributedsystemproject.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FailureDetectorComponent {

    @Autowired private JdbcTemplate jdbcNode1;
    @Autowired private JdbcTemplate jdbcNode2;
    @Autowired private JdbcTemplate jdbcNode3;

    // Lưu trạng thái trên RAM (ACTIVE, DOWN, RECOVERING)
    public final ConcurrentHashMap<String, String> nodeStatusMap = new ConcurrentHashMap<>();

    public FailureDetectorComponent() {
        nodeStatusMap.put("NODE_1", "ACTIVE");
        nodeStatusMap.put("NODE_2", "ACTIVE");
        nodeStatusMap.put("NODE_3", "ACTIVE");
    }

    // Gửi Heartbeat mỗi 3 giây
    @Scheduled(fixedRate = 3000)
    public void checkNodesHealth() {
        checkPing("NODE_1", jdbcNode1);
        checkPing("NODE_2", jdbcNode2);
        checkPing("NODE_3", jdbcNode3);
    }

    private void checkPing(String nodeId, JdbcTemplate jdbcTemplate) {
        // Không ping nếu node đang tự phục hồi dữ liệu
        if ("RECOVERING".equals(nodeStatusMap.get(nodeId))) return;

        try {
            jdbcTemplate.setQueryTimeout(1);
            jdbcTemplate.execute("SELECT 1"); // Ping nhẹ

            // Nếu sống lại sau khi sập, chuyển sang DOWN để đợi gọi hàm Recover
            if (!"DOWN".equals(nodeStatusMap.get(nodeId)) && !"ACTIVE".equals(nodeStatusMap.get(nodeId))) {
                nodeStatusMap.put(nodeId, "ACTIVE");
            }
        } catch (Exception e) {
            if (!"DOWN".equals(nodeStatusMap.get(nodeId))) {
                nodeStatusMap.put(nodeId, "DOWN");
                System.err.println("❌ " + nodeId + " SẬP (Không phản hồi). Trạng thái -> DOWN");
            }
        }
    }
}