package com.example.distributedsystemproject.controller;

import com.example.distributedsystemproject.component.FailureDetectorComponent;
import com.example.distributedsystemproject.model.RecoveryLog;
import com.example.distributedsystemproject.model.StockLevel;
import com.example.distributedsystemproject.model.StockView;
import com.example.distributedsystemproject.service.CoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @Autowired private CoordinatorService coordinatorService;
    @Autowired private FailureDetectorComponent failureDetectorComponent;

    @GetMapping("/stock/{sku}")
    public StockView getStock(@PathVariable String sku, @RequestParam(defaultValue = "FULL") String replicationMode) {
        return coordinatorService.readStock(sku, replicationMode);
    }

    @PostMapping("/stock")
    public String writeStock(@RequestParam String sku, @RequestParam int quantity, @RequestParam(defaultValue = "FULL") String replicationMode) {
        coordinatorService.writeStock(sku, quantity, "WH-MAIN", replicationMode);
        return "Đã ghi Stock thành công vào các Node ACTIVE.";
    }

    @GetMapping("/status")
    public Map<String, String> getClusterStatus() {
        return failureDetectorComponent.nodeStatusMap;
    }

    @PostMapping("/node/recover/{nodeId}")
    public String recoverNode(@PathVariable String nodeId) {
        coordinatorService.processRecovery(nodeId);
        return "Node " + nodeId + " đang bắt đầu RECOVERING bất đồng bộ. Hãy gọi API GET ngay bây giờ để kiểm tra việc ngăn chặn Stale Read!";
    }

    @PostMapping("/node/offline/{nodeId}")
    public String setNodeOffline(@PathVariable String nodeId) {
        coordinatorService.setNodeOffline(nodeId);
        return "Node " + nodeId + " đã được giả lập OFFLINE (Bảng stock_levels đổi tên thành stock_levels_offline).";
    }

    @PostMapping("/node/online/{nodeId}")
    public String setNodeOnline(@PathVariable String nodeId) {
        coordinatorService.setNodeOnline(nodeId);
        return "Node " + nodeId + " đã được giả lập ONLINE trở lại (Bảng stock_levels được khôi phục). Detector sẽ tự động kích hoạt Recovery!";
    }

    @GetMapping("/node/stocks/{nodeId}")
    public List<StockLevel> getNodeStocks(@PathVariable String nodeId) {
        return coordinatorService.getNodeStocks(nodeId);
    }

    @GetMapping("/node/logs/{nodeId}")
    public List<RecoveryLog> getNodeRecoveryLogs(@PathVariable String nodeId) {
        return coordinatorService.getNodeRecoveryLogs(nodeId);
    }
}