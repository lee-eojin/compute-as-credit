package com.yourco.compute.billing.ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
  Optional<LedgerAccount> findByUserIdAndType(Long userId, LedgerAccount.Type type);
}
