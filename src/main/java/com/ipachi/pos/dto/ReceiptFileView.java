// src/main/java/com/ipachi/pos/dto/ReceiptFileView.java
package com.ipachi.pos.dto;

public record ReceiptFileView(
        String fileName,
        String contentType,
        byte[] bytes
) {}
