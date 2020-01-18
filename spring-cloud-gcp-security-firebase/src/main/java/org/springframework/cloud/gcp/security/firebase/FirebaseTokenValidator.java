/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.gcp.security.firebase;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

/**
 * Validates Firebase tokens using the guidelines presented here: https://firebase.google.com/docs/auth/admin/verify-id-tokens
 *
 * @author Vinicius Carvalho
 * @since 1.3
 */
public class FirebaseTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final String projectId;

    private static final Duration DEFAULT_MAX_CLOCK_SKEW = Duration.of(60, ChronoUnit.SECONDS);

    private final Duration clockSkew;

    private Clock clock = Clock.systemUTC();


    public FirebaseTokenValidator(String projectId) {
        this(projectId, DEFAULT_MAX_CLOCK_SKEW);
    }

    public FirebaseTokenValidator(String projectId, Duration clockSkew) {
        this.projectId = projectId;
        this.clockSkew = clockSkew;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<OAuth2Error> errors = new LinkedList<>();
        validateAudience(errors, token);
        validateIssuedAt(errors, token);
        return OAuth2TokenValidatorResult.failure(errors);
    }


    private void validateIssuedAt(List<OAuth2Error> errors, Jwt token){
        Instant issuedAt = token.getIssuedAt();
        if (issuedAt != null) {
            if (Instant.now(this.clock).plus(clockSkew).isBefore(issuedAt)) {
                errors.add(new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST,
                        String.format("iat claim header must be in the past"),
                        "https://tools.ietf.org/html/rfc6750#section-3.1"));
            }
        }
    }

    private void validateAudience(List<OAuth2Error> errors, Jwt token){
        List<String> audiences = token.getAudience();
        if(audiences != null){
            for(String audience : audiences){
                if(audience.equals(projectId)){
                    return;
                }
            }
        }
        errors.add(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_REQUEST,
                "This aud claim is not equal to the configured audience",
                "https://tools.ietf.org/html/rfc6750#section-3.1"));
    }
}
