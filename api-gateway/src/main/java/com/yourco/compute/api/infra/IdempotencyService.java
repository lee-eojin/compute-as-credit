package com.yourco.compute.api.infra;

import com.yourco.compute.domain.model.IdempotencyKey;
import com.yourco.compute.domain.repo.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {
  private final IdempotencyKeyRepository repo;
  public IdempotencyService(IdempotencyKeyRepository repo){ this.repo = repo; }

  @Transactional
  public Optional<Long> findJob(String key, String scope){
    return repo.findByKeyAndScope(key, scope).map(IdempotencyKey::getJobId);
  }
  @Transactional
  public void remember(String key, String scope, Long jobId){
    if (repo.findByKeyAndScope(key, scope).isPresent()) return;
    repo.save(new IdempotencyKey(key, scope, jobId));
  }
}
