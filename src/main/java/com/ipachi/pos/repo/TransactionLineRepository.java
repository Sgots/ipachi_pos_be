package com.ipachi.pos.repo;


import com.ipachi.pos.model.TransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLineRepository extends JpaRepository<TransactionLine, Long> { }
