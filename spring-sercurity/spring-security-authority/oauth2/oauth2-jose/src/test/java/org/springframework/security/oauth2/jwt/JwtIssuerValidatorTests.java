/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.security.oauth2.jwt;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Josh Cummings
 * @since 5.1
 */
public class JwtIssuerValidatorTests {

	private static final String ISSUER = "https://issuer";

	private final JwtIssuerValidator validator = new JwtIssuerValidator(ISSUER);

	@Test
	public void validateWhenIssuerMatchesThenReturnsSuccess() {
		Jwt jwt = TestJwts.jwt().claim("iss", ISSUER).build();
		// @formatter:off
		assertThat(this.validator.validate(jwt))
				.isEqualTo(OAuth2TokenValidatorResult.success());
		// @formatter:on
	}

	@Test
	public void validateWhenIssuerUrlMatchesThenReturnsSuccess() throws MalformedURLException {
		Jwt jwt = TestJwts.jwt().claim("iss", new URL(ISSUER)).build();

		assertThat(this.validator.validate(jwt)).isEqualTo(OAuth2TokenValidatorResult.success());
	}

	@Test
	public void validateWhenIssuerMismatchesThenReturnsError() {
		Jwt jwt = TestJwts.jwt().claim(JwtClaimNames.ISS, "https://other").build();
		OAuth2TokenValidatorResult result = this.validator.validate(jwt);
		assertThat(result.getErrors()).isNotEmpty();
	}

	@Test
	public void validateWhenIssuerUrlMismatchesThenReturnsError() throws MalformedURLException {
		Jwt jwt = TestJwts.jwt().claim(JwtClaimNames.ISS, new URL("https://other")).build();

		OAuth2TokenValidatorResult result = this.validator.validate(jwt);

		assertThat(result.getErrors()).isNotEmpty();
	}

	@Test
	public void validateWhenJwtHasNoIssuerThenReturnsError() {
		Jwt jwt = TestJwts.jwt().claim(JwtClaimNames.AUD, "https://aud").build();
		OAuth2TokenValidatorResult result = this.validator.validate(jwt);
		assertThat(result.getErrors()).isNotEmpty();
	}

	// gh-6073
	@Test
	public void validateWhenIssuerMatchesAndIsNotAUriThenReturnsSuccess() {
		Jwt jwt = TestJwts.jwt().claim(JwtClaimNames.ISS, "issuer").build();
		JwtIssuerValidator validator = new JwtIssuerValidator("issuer");
		// @formatter:off
		assertThat(validator.validate(jwt))
				.isEqualTo(OAuth2TokenValidatorResult.success());
		// @formatter:on
	}

	@Test
	public void validateWhenJwtIsNullThenThrowsIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.validator.validate(null));
		// @formatter:on
	}

	@Test
	public void constructorWhenNullIssuerIsGivenThenThrowsIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JwtIssuerValidator(null));
		// @formatter:on
	}

}
