package com.ipachi.pos.service;

import com.ipachi.pos.dto.CheckoutRequest;
import com.ipachi.pos.dto.TillItem;
import com.ipachi.pos.model.Transaction;
import com.ipachi.pos.model.TransactionLine;
import com.ipachi.pos.repo.InventoryItemRepository;
import com.ipachi.pos.repo.TransactionLineRepository;
import com.ipachi.pos.repo.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final TransactionRepository txRepo;
    private final TransactionLineRepository lineRepo;
    private final InventoryItemRepository invRepo;

    public CheckoutService(TransactionRepository txRepo,
                           TransactionLineRepository lineRepo,
                           InventoryItemRepository invRepo) {
        this.txRepo = txRepo;
        this.lineRepo = lineRepo;
        this.invRepo = invRepo;
    }

    /**
     * Persist a transaction and its line items, update inventory levels,
     * and return the saved Transaction (with generated ID and total).
     */
    @Transactional
    public Transaction checkout(CheckoutRequest req) {
        // 1) Create transaction head
        Transaction tx = new Transaction();
        tx.setCustomerName(req.getCustomerName() != null && !req.getCustomerName().isBlank()
                ? req.getCustomerName()
                : "Walk-in");
        tx = txRepo.save(tx);

        // 2) Persist lines + update inventory + compute total
        BigDecimal total = BigDecimal.ZERO;
        List<TillItem> items = req.getItems();

        if (items != null) {
            for (TillItem it : items) {
                BigDecimal unit = safe(it.getPrice());
                int qty = it.getQty() == null ? 0 : it.getQty();
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));

                TransactionLine line = new TransactionLine();
                line.setTransaction(tx);
                line.setSku(it.getSku());
                line.setName(it.getName() != null ? it.getName() : it.getSku());
                line.setUnitPrice(unit);
                line.setQty(qty);
                line.setLineTotal(lineTotal);
                lineRepo.save(line);

                total = total.add(lineTotal);

                // Decrement inventory if SKU exists
                if (it.getSku() != null && !it.getSku().isBlank()) {
                    invRepo.findBySku(it.getSku()).ifPresent(inv -> {
                        int newQty = Math.max(0, (inv.getQuantity() == null ? 0 : inv.getQuantity()) - qty);
                        inv.setQuantity(newQty);
                        invRepo.save(inv);
                    });
                }
            }
        }

        // 3) Finalize transaction total
        tx.setTotal(total);
        return txRepo.save(tx);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
