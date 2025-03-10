package com.javaSaga.InventoryService;

import lombok.Data;

@Data
class InventoryItem {
    private Long id;
    private String name;
    private int availableQuantity;
    private int reservedQuantity;

    public InventoryItem(Long id, String name, int availableQuantity) {
        this.id = id;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }
}
