package com.yourco.compute.api.infra;

import com.yourco.compute.domain.model.IdempotencyKey;
import com.yourco.compute.domain.repo.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Keys are scoped to the user who sent them, so one caller's key cannot collide with another's.
 */
@Service
public class IdempotencyService {
  private final IdempotencyKeyRepository repo;
  public IdempotencyService(IdempotencyKeyRepository repo){ this.repo = repo; }

  @Transactional(readOnly = true)
  public Optional<Long> findJob(String key, String scope, long userId){
    return repo.findByKeyAndScopeAndUserId(key, scope, userId).map(IdempotencyKey::getJobId);
  }

  /**
   * Inserts without looking first and flushes straight away. A prior read cannot rule out a
   * concurrent writer, so the unique index decides the winner: the loser sees a
   * {@link org.springframework.dao.DataIntegrityViolationException} here, while the work it did
   * earlier in the same transaction is still uncommitted and rolls back with it.
   */
  @Transactional
  public void remember(String key, String scope, long userId, Long jobId){
    repo.saveAndFlush(new IdempotencyKey(key, scope, userId, jobId));
  }
}
