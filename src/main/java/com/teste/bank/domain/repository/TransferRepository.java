package com.teste.bank.domain.repository;

import com.teste.bank.domain.model.Transfer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    @Query("select t from Transfer t where t.fromAccountId = :accountId or t.toAccountId = :accountId")
    Page<Transfer> findStatementByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
