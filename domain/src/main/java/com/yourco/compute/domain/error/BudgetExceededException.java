package com.yourco.compute.domain.error;

import java.math.BigDecimal;

public class BudgetExceededException extends RuntimeException {
  public BudgetExceededException(BigDecimal hold, BigDecimal maxBudget) {
    super("Required hold " + hold.toPlainString()
        + " exceeds maxBudget " + maxBudget.toPlainString());
  }
}
