package com.yourco.compute.api.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.regex.Pattern;

/**
 * Resolves the billable user from the bearer token. The subject claim is the user id, so no
 * request body can redirect a charge to another account.
 */
public final class CallerId {
  // Long.parseLong would also accept "+1", " 1", "007" and Unicode digits, collapsing distinct
  // subjects onto one billable user. One canonical spelling per id instead.
  private static final Pattern USER_ID = Pattern.compile("0|[1-9][0-9]{0,18}");

  private CallerId() {}

  public static long of(Jwt jwt) {
    if (jwt == null) {
      throw new UnauthorizedCallerException("No authenticated caller");
    }
    String subject = jwt.getSubject();
    if (subject == null || !USER_ID.matcher(subject).matches()) {
      throw new UnauthorizedCallerException("Token subject is not a numeric user id: " + subject);
    }
    return Long.parseLong(subject);
  }
}
