package com.yourco.compute.billing.ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerPostingRepository extends JpaRepository<LedgerPosting, Long> {}
