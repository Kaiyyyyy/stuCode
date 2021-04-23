/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.security.oauth2.client.oidc.authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Winch
 * @author Joe Grandja
 * @since 5.1
 */
public class OidcIdTokenValidatorTests {

	private ClientRegistration.Builder registration = TestClientRegistrations.clientRegistration();

	private Map<String, Object> headers = new HashMap<>();

	private Map<String, Object> claims = new HashMap<>();

	private Instant issuedAt = Instant.now();

	private Instant expiresAt = this.issuedAt.plusSeconds(3600);

	private Duration clockSkew = Duration.ofSeconds(60);

	@Before
	public void setup() {
		this.headers.put("alg", JwsAlgorithms.RS256);
		this.claims.put(IdTokenClaimNames.ISS, "https://example.com");
		this.claims.put(IdTokenClaimNames.SUB, "rob");
		this.claims.put(IdTokenClaimNames.AUD, Collections.singletonList("client-id"));
	}

	@Test
	public void validateWhenValidThenNoErrors() {
		assertThat(this.validateIdToken()).isEmpty();
	}

	@Test
	public void setClockSkewWhenNullThenThrowIllegalArgumentException() {
		OidcIdTokenValidator idTokenValidator = new OidcIdTokenValidator(this.registration.build());
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> idTokenValidator.setClockSkew(null));
		// @formatter:on
	}

	@Test
	public void setClockSkewWhenNegativeSecondsThenThrowIllegalArgumentException() {
		OidcIdTokenValidator idTokenValidator = new OidcIdTokenValidator(this.registration.build());
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> idTokenValidator.setClockSkew(Duration.ofSeconds(-1)));
		// @formatter:on
	}

	@Test
	public void setClockWhenNullThenThrowIllegalArgumentException() {
		OidcIdTokenValidator idTokenValidator = new OidcIdTokenValidator(this.registration.build());
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> idTokenValidator.setClock(null));
		// @formatter:on
	}

	@Test
	public void validateWhenIssuerNullThenHasErrors() {
		this.claims.remove(IdTokenClaimNames.ISS);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.ISS));
		// @formatter:on
	}

	@Test
	public void validateWhenMetadataIssuerMismatchThenHasErrors() {
		/*
		 * When the issuer is set in the provider metadata, and it does not match the
		 * issuer in the ID Token, the validation must fail
		 */
		this.registration = this.registration.issuerUri("https://somethingelse.com");
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.ISS));
		// @formatter:on
	}

	@Test
	public void validateWhenMetadataIssuerMatchThenNoErrors() {
		/*
		 * When the issuer is set in the provider metadata, and it does match the issuer
		 * in the ID Token, the validation must succeed
		 */
		this.registration = this.registration.issuerUri("https://example.com");
		assertThat(this.validateIdToken()).isEmpty();
	}

	@Test
	public void validateWhenSubNullThenHasErrors() {
		this.claims.remove(IdTokenClaimNames.SUB);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.SUB));
		// @formatter:on
	}

	@Test
	public void validateWhenAudNullThenHasErrors() {
		this.claims.remove(IdTokenClaimNames.AUD);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.AUD));
		// @formatter:on
	}

	@Test
	public void validateWhenIssuedAtNullThenHasErrors() {
		this.issuedAt = null;
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.IAT));
		// @formatter:on
	}

	@Test
	public void validateWhenExpiresAtNullThenHasErrors() {
		this.expiresAt = null;
		assertThat(this.validateIdToken()).hasSize(1).extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.EXP));
	}

	@Test
	public void validateWhenAudMultipleAndAzpNullThenHasErrors() {
		this.claims.put(IdTokenClaimNames.AUD, Arrays.asList("client-id", "other"));
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.AZP));
		// @formatter:on
	}

	@Test
	public void validateWhenAzpNotClientIdThenHasErrors() {
		this.claims.put(IdTokenClaimNames.AZP, "other");
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.AZP));
		// @formatter:on
	}

	@Test
	public void validateWhenMultipleAudAzpClientIdThenNoErrors() {
		this.claims.put(IdTokenClaimNames.AUD, Arrays.asList("client-id", "other"));
		this.claims.put(IdTokenClaimNames.AZP, "client-id");
		assertThat(this.validateIdToken()).isEmpty();
	}

	@Test
	public void validateWhenMultipleAudAzpNotClientIdThenHasErrors() {
		this.claims.put(IdTokenClaimNames.AUD, Arrays.asList("client-id-1", "client-id-2"));
		this.claims.put(IdTokenClaimNames.AZP, "other-client");
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.AZP));
		// @formatter:on
	}

	@Test
	public void validateWhenAudNotClientIdThenHasErrors() {
		this.claims.put(IdTokenClaimNames.AUD, Collections.singletonList("other-client"));
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.AUD));
		// @formatter:on
	}

	@Test
	public void validateWhenExpiredAnd60secClockSkewThenNoErrors() {
		this.issuedAt = Instant.now().minus(Duration.ofSeconds(60));
		this.expiresAt = this.issuedAt.plus(Duration.ofSeconds(30));
		this.clockSkew = Duration.ofSeconds(60);
		assertThat(this.validateIdToken()).isEmpty();
	}

	@Test
	public void validateWhenExpiredAnd0secClockSkewThenHasErrors() {
		this.issuedAt = Instant.now().minus(Duration.ofSeconds(60));
		this.expiresAt = this.issuedAt.plus(Duration.ofSeconds(30));
		this.clockSkew = Duration.ofSeconds(0);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.EXP));
		// @formatter:on
	}

	@Test
	public void validateWhenIssuedAt5minAheadAnd5minClockSkewThenNoErrors() {
		this.issuedAt = Instant.now().plus(Duration.ofMinutes(5));
		this.expiresAt = this.issuedAt.plus(Duration.ofSeconds(60));
		this.clockSkew = Duration.ofMinutes(5);
		assertThat(this.validateIdToken()).isEmpty();
	}

	@Test
	public void validateWhenIssuedAt1minAheadAnd0minClockSkewThenHasErrors() {
		this.issuedAt = Instant.now().plus(Duration.ofMinutes(1));
		this.expiresAt = this.issuedAt.plus(Duration.ofSeconds(60));
		this.clockSkew = Duration.ofMinutes(0);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.IAT));
		// @formatter:on
	}

	@Test
	public void validateWhenExpiresAtBeforeNowThenHasErrors() {
		this.issuedAt = Instant.now().minus(Duration.ofSeconds(10));
		this.expiresAt = this.issuedAt.plus(Duration.ofSeconds(5));
		this.clockSkew = Duration.ofSeconds(0);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.EXP));
		// @formatter:on
	}

	@Test
	public void validateWhenMissingClaimsThenHasErrors() {
		this.claims.remove(IdTokenClaimNames.SUB);
		this.claims.remove(IdTokenClaimNames.AUD);
		this.issuedAt = null;
		this.expiresAt = null;
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.SUB))
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.AUD))
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.IAT))
				.allMatch((msg) -> msg.contains(IdTokenClaimNames.EXP));
		// @formatter:on
	}

	@Test
	public void validateFormatError() {
		this.claims.remove(IdTokenClaimNames.SUB);
		this.claims.remove(IdTokenClaimNames.AUD);
		// @formatter:off
		assertThat(this.validateIdToken())
				.hasSize(1)
				.extracting(OAuth2Error::getDescription)
				.allMatch((msg) -> msg.equals("The ID Token contains invalid claims: {sub=null, aud=null}"));
		// @formatter:on
	}

	private Collection<OAuth2Error> validateIdToken() {
		// @formatter:off
		Jwt idToken = Jwt.withTokenValue("token")
				.issuedAt(this.issuedAt)
				.expiresAt(this.expiresAt)
				.headers((h) -> h.putAll(this.headers))
				.claims((c) -> c.putAll(this.claims))
				.build();
		// @formatter:on
		OidcIdTokenValidator validator = new OidcIdTokenValidator(this.registration.build());
		validator.setClockSkew(this.clockSkew);
		return validator.validate(idToken).getErrors();
	}

}
