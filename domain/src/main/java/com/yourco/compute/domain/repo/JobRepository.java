package com.yourco.compute.domain.repo;

import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
  List<Job> findByUserIdOrderByCreatedAtDesc(Long userId);
  List<Job> findByStatus(JobStatus status);
}
