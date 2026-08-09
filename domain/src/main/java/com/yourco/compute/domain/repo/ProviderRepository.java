package com.yourco.compute.domain.repo;

import com.yourco.compute.domain.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
  Optional<Provider> findByName(String name);
}
