package com.yourco.compute.api.error;

import com.yourco.compute.api.security.UnauthorizedCallerException;
import com.yourco.compute.domain.error.BudgetExceededException;
import com.yourco.compute.domain.error.JobNotFoundException;
import com.yourco.compute.domain.error.NoProviderAvailableException;
import com.yourco.compute.domain.error.UnsupportedResourceHintException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(JobNotFoundException.class)
  public ProblemDetail jobNotFound(JobNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  /** The hint parsed cleanly but named a region or GPU the platform does not broker. */
  @ExceptionHandler(UnsupportedResourceHintException.class)
  public ProblemDetail unsupportedResourceHint(UnsupportedResourceHintException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(BudgetExceededException.class)
  public ProblemDetail budgetExceeded(BudgetExceededException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
  }

  @ExceptionHandler(UnauthorizedCallerException.class)
  public ProblemDetail unauthorizedCaller(UnauthorizedCallerException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  /**
   * Two concurrent first-time requests can race on the ledger account and idempotency key unique
   * constraints. A conflict tells the caller to retry, where a 500 would suggest the request itself
   * was bad.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail conflict(DataIntegrityViolationException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflicting concurrent request, retry");
  }

  @ExceptionHandler(NoProviderAvailableException.class)
  public ProblemDetail noProvider(NoProviderAvailableException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
  }

  /** A provider that is unreachable or answers badly is an upstream fault, not a bad request. */
  @ExceptionHandler(RestClientException.class)
  public ProblemDetail providerUnreachable(RestClientException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Provider call failed: " + e.getMessage());
  }
}
