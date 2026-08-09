package com.yourco.compute.billing.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(LedgerService.class)
class LedgerServiceTest {

  @Autowired LedgerService ledger;
  @Autowired LedgerAccountRepository accounts;
  @Autowired LedgerPostingRepository postings;

  @Test
  void aHoldMovesCreditBetweenTwoDistinctAccounts() {
    ledger.hold(UUID.randomUUID(), 7L, new BigDecimal("12.50"), 99L);

    LedgerAccount balance = account(7L, "balance");
    LedgerAccount hold = account(7L, "hold");

    assertThat(balance.getId()).isNotEqualTo(hold.getId());
    assertThat(postings.findAll())
        .hasSize(2)
        .extracting(LedgerPosting::getAccountId)
        .containsExactlyInAnyOrder(balance.getId(), hold.getId());
  }

  @Test
  void aDebitMovesTheHoldToPlatformRevenue() {
    ledger.debit(UUID.randomUUID(), 7L, new BigDecimal("12.50"), 99L);

    assertThat(account(7L, "hold").getId()).isNotEqualTo(account(0L, "revenue").getId());
    assertThat(postings.findAll()).hasSize(2);
  }

  @Test
  void oneAccountPerUserTypeAndNameIsEnforcedBySchema() {
    ledger.hold(UUID.randomUUID(), 7L, new BigDecimal("12.50"), 99L);
    LedgerAccount duplicate = new LedgerAccount();
    duplicate.setUserId(7L);
    duplicate.setType(LedgerAccount.Type.LIABILITY);
    duplicate.setName("hold");

    assertThatThrownBy(() -> accounts.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void replayingAnEntryKeyPostsNothingExtra() {
    UUID key = UUID.randomUUID();

    ledger.hold(key, 7L, new BigDecimal("12.50"), 99L);
    ledger.hold(key, 7L, new BigDecimal("12.50"), 99L);

    assertThat(postings.findAll()).hasSize(2);
  }

  private LedgerAccount account(long userId, String name) {
    LedgerAccount.Type type = "revenue".equals(name) ? LedgerAccount.Type.REVENUE : LedgerAccount.Type.LIABILITY;
    return accounts.findByUserIdAndTypeAndName(userId, type, name).orElseThrow();
  }
}
