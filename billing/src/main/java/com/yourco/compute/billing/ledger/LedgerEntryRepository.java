package com.yourco.compute.billing.ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
  Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);
}
