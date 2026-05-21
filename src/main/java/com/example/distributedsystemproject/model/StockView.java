package com.example.distributedsystemproject.model;

public class StockView {
    private final String sku;
    private final int quantity;
    private final String warehouseId;
    private final String servedByNode;

    public StockView(String sku, int quantity, String warehouseId, String servedByNode) {
        this.sku = sku;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.servedByNode = servedByNode;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getWarehouseId() { return warehouseId; }
    public String getServedByNode() { return servedByNode; }
}