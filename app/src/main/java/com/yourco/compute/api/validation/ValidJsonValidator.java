package com.yourco.compute.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidJsonValidator implements ConstraintValidator<ValidJson, String> {
  private final ObjectReader reader;

  public ValidJsonValidator(ObjectMapper mapper) {
    // readTree stops at the first complete value, so trailing tokens have to be rejected explicitly
    // or "{} junk" reaches the json column and fails at INSERT instead of at validation.
    this.reader = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    try {
      return reader.readTree(value).isObject();
    } catch (JsonProcessingException e) {
      return false;
    }
  }
}
