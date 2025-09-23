package com.ipachi.pos.service;

import com.ipachi.pos.dto.TxnLineDto;
import com.ipachi.pos.repo.TransactionLineRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TransactionQueryService {

    private final TransactionLineRepository lineRepo;

    public TransactionQueryService(TransactionLineRepository lineRepo) {
        this.lineRepo = lineRepo;
    }

    public List<TxnLineDto> searchLinesByBusiness(
            Long businessId,
            String sku,
            String name,
            Long txId,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            Integer minQty,
            Integer maxQty
    ) {
        return lineRepo.searchLinesForBusiness(
                businessId,
                nullIfBlank(sku),
                nullIfBlank(name),
                txId,
                dateFrom,
                dateTo,
                minQty,
                maxQty
        );
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
