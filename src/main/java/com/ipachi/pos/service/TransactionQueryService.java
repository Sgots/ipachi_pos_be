package com.ipachi.pos.service;

import com.ipachi.pos.dto.CombinedTxnDto;
import com.ipachi.pos.dto.TxnLineDto;
import com.ipachi.pos.model.StaffMember;
import com.ipachi.pos.model.User;
import com.ipachi.pos.repo.StaffMemberRepository;
import com.ipachi.pos.repo.TransactionLineRepository;
import com.ipachi.pos.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class TransactionQueryService {

    private final TransactionLineRepository lineRepo;
    private final UserRepository userRepository;
    private final StaffMemberRepository staffMemberRepository;

    public TransactionQueryService(TransactionLineRepository lineRepo,UserRepository userRepository, StaffMemberRepository staffMemberRepository) {
        this.lineRepo = lineRepo;
        this.userRepository = userRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public List<TxnLineDto> searchLinesByBusiness(
            Long businessId, String sku, String name, Long txId,
            OffsetDateTime dateFrom, OffsetDateTime dateTo, Integer minQty, Integer maxQty
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

    /** Group the flat rows into CombinedTxnDto by txId. */
    public List<CombinedTxnDto> searchCombinedByBusiness(
            Long businessId, String sku, String name, Long txId,
            OffsetDateTime dateFrom, OffsetDateTime dateTo, Integer minQty, Integer maxQty
    ) {
        List<TxnLineDto> rows = searchLinesByBusiness(businessId, sku, name, txId, dateFrom, dateTo, minQty, maxQty);

        // preserve encounter order
        Map<Long, CombinedTxnDto> grouped = new LinkedHashMap<>();

        for (TxnLineDto line : rows) {
            Long id = line.getTxId();

            CombinedTxnDto dto = grouped.computeIfAbsent(
                    id,
                    k -> new CombinedTxnDto(k, null, null, null, null, new ArrayList<>())
            );
            User user = userRepository.findById(line.getCreatedByUserId()).orElse(null);
            StaffMember staffMember = staffMemberRepository.findByUserId(user.getId()).orElse(null);;

            // Set header fields once (from the first encountered line)
            if (dto.getCreatedAt() == null) dto.setCreatedAt(line.getDate());
            if (dto.getCreatedByUserId() == null) {
                assert user != null;
                dto.setCreatedByUserId(user.getUsername());
            }
            if (dto.getTerminalId() == null) dto.setTerminalId(line.getTerminalId());
            if (dto.getStaffName() == null) dto.setStaffName(staffMember.getFirstName());


            dto.getLines().add(line);
        }

        return new ArrayList<>(grouped.values());
    }
}
