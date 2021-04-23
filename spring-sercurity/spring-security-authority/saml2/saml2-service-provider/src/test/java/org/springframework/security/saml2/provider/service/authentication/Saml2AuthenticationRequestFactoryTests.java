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

package org.springframework.security.saml2.provider.service.authentication;

import java.util.UUID;

import org.junit.Test;

import org.springframework.security.saml2.credentials.TestSaml2X509Credentials;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Saml2AuthenticationRequestFactory} default interface methods
 */
public class Saml2AuthenticationRequestFactoryTests {

	private RelyingPartyRegistration registration = RelyingPartyRegistration.withRegistrationId("id")
			.assertionConsumerServiceUrlTemplate("template")
			.providerDetails((c) -> c.webSsoUrl("https://example.com/destination"))
			.providerDetails((c) -> c.entityId("remote-entity-id")).localEntityIdTemplate("local-entity-id")
			.credentials((c) -> c.add(TestSaml2X509Credentials.relyingPartySigningCredential())).build();

	@Test
	public void createAuthenticationRequestParametersWhenRedirectDefaultIsUsedMessageIsDeflatedAndEncoded() {
		final String value = "Test String: " + UUID.randomUUID().toString();
		Saml2AuthenticationRequestFactory factory = (request) -> value;
		Saml2AuthenticationRequestContext request = Saml2AuthenticationRequestContext.builder()
				.relyingPartyRegistration(this.registration).issuer("https://example.com/issuer")
				.assertionConsumerServiceUrl("https://example.com/acs-url").build();
		Saml2RedirectAuthenticationRequest response = factory.createRedirectAuthenticationRequest(request);
		String resultValue = response.getSamlRequest();
		byte[] decoded = Saml2Utils.samlDecode(resultValue);
		String inflated = Saml2Utils.samlInflate(decoded);
		assertThat(inflated).isEqualTo(value);
	}

	@Test
	public void createAuthenticationRequestParametersWhenPostDefaultIsUsedMessageIsEncoded() {
		final String value = "Test String: " + UUID.randomUUID().toString();
		Saml2AuthenticationRequestFactory factory = (request) -> value;
		Saml2AuthenticationRequestContext request = Saml2AuthenticationRequestContext.builder()
				.relyingPartyRegistration(this.registration).issuer("https://example.com/issuer")
				.assertionConsumerServiceUrl("https://example.com/acs-url").build();
		Saml2PostAuthenticationRequest response = factory.createPostAuthenticationRequest(request);
		String resultValue = response.getSamlRequest();
		byte[] decoded = Saml2Utils.samlDecode(resultValue);
		assertThat(new String(decoded)).isEqualTo(value);
	}

}
