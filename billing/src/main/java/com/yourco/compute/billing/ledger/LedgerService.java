package com.yourco.compute.billing.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LedgerService {
  private final LedgerEntryRepository entryRepo;
  private final LedgerPostingRepository postingRepo;
  private final LedgerAccountRepository accountRepo;

  public LedgerService(LedgerEntryRepository e, LedgerPostingRepository p, LedgerAccountRepository a){
    this.entryRepo = e;
    this.postingRepo = p;
    this.accountRepo = a;
  }

  @Transactional
  public void hold(UUID idemKey, long userId, BigDecimal amount, Long jobId) {
    if (entryRepo.findByIdempotencyKey(idemKey.toString()).isPresent()) return;

    LedgerEntry entry = new LedgerEntry();
    entry.setKind(LedgerEntry.Kind.HOLD);
    entry.setJobId(jobId);
    entry.setIdempotencyKey(idemKey.toString());
    entry = entryRepo.save(entry);

    LedgerAccount userBalance = getOrCreateNamed(userId, LedgerAccount.Type.LIABILITY, "balance");
    LedgerAccount userHold = getOrCreateNamed(userId, LedgerAccount.Type.LIABILITY, "hold");

    post(entry.getId(), userBalance.getId(), LedgerPosting.Side.DEBIT,  amount);
    post(entry.getId(), userHold.getId(),    LedgerPosting.Side.CREDIT, amount);
  }

  @Transactional
  public void debit(UUID idemKey, long userId, BigDecimal amount, Long jobId) {
    if (entryRepo.findByIdempotencyKey(idemKey.toString()).isPresent()) return;

    LedgerEntry entry = new LedgerEntry();
    entry.setKind(LedgerEntry.Kind.DEBIT);
    entry.setJobId(jobId);
    entry.setIdempotencyKey(idemKey.toString());
    entry = entryRepo.save(entry);

    LedgerAccount userHold = getOrCreateNamed(userId, LedgerAccount.Type.LIABILITY, "hold");
    LedgerAccount platformRevenue = getOrCreateNamed(0L, LedgerAccount.Type.REVENUE, "revenue");

    post(entry.getId(), userHold.getId(),        LedgerPosting.Side.DEBIT,  amount);
    post(entry.getId(), platformRevenue.getId(), LedgerPosting.Side.CREDIT, amount);
  }

  @Transactional
  public void refund(UUID idemKey, long userId, BigDecimal amount, Long jobId) {
    if (entryRepo.findByIdempotencyKey(idemKey.toString()).isPresent()) return;

    LedgerEntry entry = new LedgerEntry();
    entry.setKind(LedgerEntry.Kind.REFUND);
    entry.setJobId(jobId);
    entry.setIdempotencyKey(idemKey.toString());
    entry = entryRepo.save(entry);

    LedgerAccount userHold = getOrCreateNamed(userId, LedgerAccount.Type.LIABILITY, "hold");
    LedgerAccount userBalance = getOrCreateNamed(userId, LedgerAccount.Type.LIABILITY, "balance");

    post(entry.getId(), userHold.getId(),    LedgerPosting.Side.DEBIT,  amount);
    post(entry.getId(), userBalance.getId(), LedgerPosting.Side.CREDIT, amount);
  }

  private LedgerAccount getOrCreateNamed(long userId, LedgerAccount.Type type, String name){
    return accountRepo.findByUserIdAndType(userId, type).orElseGet(() -> {
      LedgerAccount account = new LedgerAccount();
      account.setUserId(userId);
      account.setType(type);
      account.setCurrency("USD");
      return accountRepo.save(account);
    });
  }

  private void post(Long entryId, Long accountId, LedgerPosting.Side side, BigDecimal amount){
    LedgerPosting posting = new LedgerPosting();
    posting.setEntryId(entryId);
    posting.setAccountId(accountId);
    posting.setSide(side);
    posting.setAmount(amount);
    postingRepo.save(posting);
  }
}
