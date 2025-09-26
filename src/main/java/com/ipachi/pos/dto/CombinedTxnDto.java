package com.ipachi.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** One transaction (txId) with all its lines */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombinedTxnDto {
    private Long txId;

    // Header fields (inferred from the first line we encounter for this txId)
    private OffsetDateTime createdAt;
    private String createdByUserId;
    private String staffName;

    private String terminalId;

    private List<TxnLineDto> lines = new ArrayList<>();
}
