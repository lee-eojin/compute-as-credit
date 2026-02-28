package com.yourco.compute.billing.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
  Optional<LedgerAccount> findByUserIdAndType(Long userId, LedgerAccount.Type type);
}

interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
  Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);
}

interface LedgerPostingRepository extends JpaRepository<LedgerPosting, Long> {
}
