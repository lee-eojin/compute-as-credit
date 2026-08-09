package com.yourco.compute.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The annotated string must be a JSON object, because it lands in a MySQL json column. */
@Documented
@Constraint(validatedBy = ValidJsonValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJson {
  String message() default "must be a JSON object";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
