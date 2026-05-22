package com.example.distributedsystemproject.repository.node1;

import com.example.distributedsystemproject.model.RecoveryLog;
import com.example.distributedsystemproject.model.StockLevel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Node1Repository extends JpaRepository<StockLevel, String> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO stock_levels (sku, quantity, warehouse_id) VALUES (:sku, :quantity, :warehouseId) ON DUPLICATE KEY UPDATE quantity = :quantity", nativeQuery = true)
    void upsertStock(@Param("sku") String sku, @Param("quantity") int quantity, @Param("warehouseId") String warehouseId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO recovery_log (target_node, sku, quantity) VALUES (:targetNode, :sku, :quantity)", nativeQuery = true)
    void insertRecoveryLog(@Param("targetNode") String targetNode, @Param("sku") String sku, @Param("quantity") int quantity);

    @Query(value = "SELECT * FROM stock_levels WHERE sku = :sku", nativeQuery = true)
    Optional<StockLevel> findStockBySku(@Param("sku") String sku);

    @Query(value = "SELECT * FROM recovery_log WHERE target_node = :targetNode", nativeQuery = true)
    List<RecoveryLog> findRecoveryLogs(@Param("targetNode") String targetNode);

    @Query(value = "SELECT * FROM recovery_log", nativeQuery = true)
    List<RecoveryLog> findAllRecoveryLogs();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM recovery_log WHERE log_id = :logId", nativeQuery = true)
    void deleteRecoveryLog(@Param("logId") Long logId);

    @Modifying
    @Transactional
    @Query(value = "RENAME TABLE stock_levels TO stock_levels_offline", nativeQuery = true)
    void setOffline();

    @Modifying
    @Transactional
    @Query(value = "RENAME TABLE stock_levels_offline TO stock_levels", nativeQuery = true)
    void setOnline();

    @Query(value = """
        SELECT CASE
            WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'stock_levels')
             AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'recovery_log')
            THEN 1 ELSE 0
        END
        """, nativeQuery = true)
    Integer ping();
}