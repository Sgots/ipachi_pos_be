package com.ipachi.pos.service;

import com.ipachi.pos.model.Customer;
import com.ipachi.pos.model.FeatureFlags;
import com.ipachi.pos.model.InventoryItem;
import com.ipachi.pos.model.Transaction;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class DataStore {
    private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    private final Map<Long, InventoryItem> inventory = new ConcurrentHashMap<>();
    private final Map<Long, FeatureFlags> customerFlags = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());
    private FeatureFlags globalFlags = new FeatureFlags(false, false, false, false);

    private final AtomicLong customerSeq = new AtomicLong(0);
    private final AtomicLong inventorySeq = new AtomicLong(0);
    private final AtomicLong transactionSeq = new AtomicLong(0);

    @PostConstruct
    public void seed() {
        // Seed customers
        addCustomer(new Customer(null, "Alice", "Smith", "267680904", "ID5318", 2719.98, null));
        addCustomer(new Customer(null, "Bob", "Johnson", "267299654", "ID7940", 2857.57, null));
        addCustomer(new Customer(null, "Cindy", "Lee", "267982492", "ID4624", 4275.89, null));

        // Seed inventory
        addInventory(new InventoryItem(null, "SKU-001", "Bottled Water 500ml", 8.50, 120));
        addInventory(new InventoryItem(null, "SKU-002", "Bread Loaf", 15.00, 50));
        addInventory(new InventoryItem(null, "SKU-003", "Chocolate Bar", 10.00, 75));

        // Seed a transaction
        transactions.add(new Transaction(nextTransactionId(), OffsetDateTime.now(), "Walk-in", 100.00));
    }

    public long nextCustomerId() { return customerSeq.incrementAndGet(); }
    public long nextInventoryId() { return inventorySeq.incrementAndGet(); }
    public long nextTransactionId() { return transactionSeq.incrementAndGet(); }

    public List<Customer> listCustomers() { return new ArrayList<>(customers.values()); }
    public Customer addCustomer(Customer c) {
        if (c.getId() == null) c.setId(nextCustomerId());
        customers.put(c.getId(), c);
        return c;
    }
    public List<Customer> searchCustomers(String q) {
        if (q == null || q.isBlank()) return listCustomers();
        String s = q.toLowerCase();
        return customers.values().stream().filter(c ->
            (c.getFirstname()!=null && c.getFirstname().toLowerCase().contains(s)) ||
            (c.getLastname()!=null && c.getLastname().toLowerCase().contains(s)) ||
            (c.getMobileNumber()!=null && c.getMobileNumber().toLowerCase().contains(s)) ||
            (c.getIdentificationNumber()!=null && c.getIdentificationNumber().toLowerCase().contains(s))
        ).collect(Collectors.toList());
    }

    public List<InventoryItem> listInventory() { return new ArrayList<>(inventory.values()); }
    public InventoryItem addInventory(InventoryItem item) {
        if (item.getId() == null) item.setId(nextInventoryId());
        inventory.put(item.getId(), item);
        return item;
    }
    public Optional<InventoryItem> findBySku(String sku) {
        return inventory.values().stream().filter(i -> i.getSku().equalsIgnoreCase(sku)).findFirst();
    }

    public FeatureFlags getGlobalFlags() { return globalFlags; }
    public void setGlobalFlags(FeatureFlags flags) { this.globalFlags = flags; }
    public FeatureFlags getCustomerFlags(long customerId) {
        return customerFlags.getOrDefault(customerId, new FeatureFlags(false, false, false, false));
    }
    public void setCustomerFlags(long customerId, FeatureFlags flags) {
        customerFlags.put(customerId, flags);
    }

    public List<Transaction> listTransactions() { return new ArrayList<>(transactions); }
    public Transaction addTransaction(Transaction t) {
        if (t.getId() == null) t.setId(nextTransactionId());
        transactions.add(t);
        return t;
    }
}
