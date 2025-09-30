// src/main/java/com/ipachi/pos/dto/RestockHistoryView.java
package com.ipachi.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RestockHistoryView(
        Long receiptId,
        OffsetDateTime receiptAt,
        String label,
        String uploadedBy,          // username of uploader (receipt.user_id)
        boolean hasFile,
        String fileUrl,

        BigDecimal openingValue,    // Σ(openingQty * sellPrice) for products in this receipt
        BigDecimal newValue,        // Σ(addedQty * sellPrice) where addedQty > 0 (linked to this receipt)
        BigDecimal closingValue     // openingValue + newValue
) {}
