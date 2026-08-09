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

  @Transactional
  public Optional<Long> findJob(String key, String scope, long userId){
    return repo.findByKeyAndScopeAndUserId(key, scope, userId).map(IdempotencyKey::getJobId);
  }
  @Transactional
  public void remember(String key, String scope, long userId, Long jobId){
    if (repo.findByKeyAndScopeAndUserId(key, scope, userId).isPresent()) return;
    repo.save(new IdempotencyKey(key, scope, userId, jobId));
  }
}
