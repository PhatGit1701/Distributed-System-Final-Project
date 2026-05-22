package com.example.distributedsystemproject.component;

import com.example.distributedsystemproject.repository.node1.Node1Repository;
import com.example.distributedsystemproject.repository.node2.Node2Repository;
import com.example.distributedsystemproject.repository.node3.Node3Repository;
import com.example.distributedsystemproject.service.CoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FailureDetectorComponent {

    @Autowired private Node1Repository node1Repository;
    @Autowired private Node2Repository node2Repository;
    @Autowired private Node3Repository node3Repository;
    @Autowired @Lazy
    private CoordinatorService coordinatorService;

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
        checkPing("NODE_1"); checkPing("NODE_2"); checkPing("NODE_3");
    }

    private void checkPing(String nodeId) {
        if ("RECOVERING".equals(nodeStatusMap.get(nodeId))) return;
        boolean alive;
        try {
            alive = switch (nodeId) {
                case "NODE_1" -> Integer.valueOf(1).equals(node1Repository.ping());
                case "NODE_2" -> Integer.valueOf(1).equals(node2Repository.ping());
                case "NODE_3" -> Integer.valueOf(1).equals(node3Repository.ping());
                default -> false;
            };
        } catch (Exception e) {
            alive = false; // Database is down/inaccessible
        }
        String currentStatus = nodeStatusMap.get(nodeId);

        if (!alive && !"DOWN".equals(currentStatus)) {
            nodeStatusMap.put(nodeId, "DOWN");
            return;
        }

        if (alive && "DOWN".equals(currentStatus)) {
            coordinatorService.processRecovery(nodeId);
            return;
        }
        if (alive && !"ACTIVE".equals(currentStatus) && !"RECOVERING".equals(currentStatus)) {
            nodeStatusMap.put(nodeId, "ACTIVE");
        }
    }
}