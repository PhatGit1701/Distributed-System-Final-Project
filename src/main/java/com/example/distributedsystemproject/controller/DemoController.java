package com.example.distributedsystemproject.controller;

import com.example.distributedsystemproject.component.FailureDetectorComponent;
import com.example.distributedsystemproject.service.CoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @Autowired private CoordinatorService coordinatorService;
    @Autowired private FailureDetectorComponent failureDetectorComponent;

    @GetMapping("/stock/{sku}")
    public Map<String, Object> getStock(@PathVariable String sku) {
        return coordinatorService.readStock(sku);
    }

    @PostMapping("/stock")
    public String writeStock(@RequestParam String sku, @RequestParam int quantity) {
        coordinatorService.writeStock(sku, quantity, "WH-MAIN");
        return "Đã ghi Stock thành công vào các Node ACTIVE.";
    }

    @GetMapping("/status")
    public Map<String, String> getClusterStatus() {
        return failureDetectorComponent.nodeStatusMap;
    }

    @PostMapping("/node/recover/{nodeId}")
    public String recoverNode(@PathVariable String nodeId) {
        if (!"DOWN".equals(failureDetectorComponent.nodeStatusMap.get(nodeId))) {
            return "Node " + nodeId + " không ở trạng thái DOWN.";
        }
        coordinatorService.processRecovery(nodeId);
        return "Node " + nodeId + " đang bắt đầu RECOVERING bất đồng bộ. Hãy gọi API GET ngay bây giờ để kiểm tra việc ngăn chặn Stale Read!";
    }
}