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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.impl.AuthnRequestUnmarshaller;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.credentials.TestSaml2X509Credentials;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.registration.TestRelyingPartyRegistrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OpenSamlAuthenticationRequestFactory}
 */
public class OpenSamlAuthenticationRequestFactoryTests {

	private OpenSamlAuthenticationRequestFactory factory;

	private Saml2AuthenticationRequestContext.Builder contextBuilder;

	private Saml2AuthenticationRequestContext context;

	private RelyingPartyRegistration.Builder relyingPartyRegistrationBuilder;

	private RelyingPartyRegistration relyingPartyRegistration;

	private AuthnRequestUnmarshaller unmarshaller;

	@Before
	public void setUp() {
		this.relyingPartyRegistrationBuilder = RelyingPartyRegistration.withRegistrationId("id")
				.assertionConsumerServiceLocation("template")
				.providerDetails((c) -> c.webSsoUrl("https://destination/sso"))
				.providerDetails((c) -> c.entityId("remote-entity-id")).localEntityIdTemplate("local-entity-id")
				.credentials((c) -> c.add(TestSaml2X509Credentials.relyingPartySigningCredential()));
		this.relyingPartyRegistration = this.relyingPartyRegistrationBuilder.build();
		this.contextBuilder = Saml2AuthenticationRequestContext.builder().issuer("https://issuer")
				.relyingPartyRegistration(this.relyingPartyRegistration)
				.assertionConsumerServiceUrl("https://issuer/sso");
		this.context = this.contextBuilder.build();
		this.factory = new OpenSamlAuthenticationRequestFactory();
		this.unmarshaller = (AuthnRequestUnmarshaller) XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
				.getUnmarshaller(AuthnRequest.DEFAULT_ELEMENT_NAME);
	}

	@Test
	public void createAuthenticationRequestWhenInvokingDeprecatedMethodThenReturnsXML() {
		Saml2AuthenticationRequest request = Saml2AuthenticationRequest.withAuthenticationRequestContext(this.context)
				.build();
		String result = this.factory.createAuthenticationRequest(request);
		assertThat(result.replace("\n", ""))
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><saml2p:AuthnRequest");
	}

	@Test
	public void createRedirectAuthenticationRequestWhenUsingContextThenAllValuesAreSet() {
		this.context = this.contextBuilder.relayState("Relay State Value").build();
		Saml2RedirectAuthenticationRequest result = this.factory.createRedirectAuthenticationRequest(this.context);
		assertThat(result.getSamlRequest()).isNotEmpty();
		assertThat(result.getRelayState()).isEqualTo("Relay State Value");
		assertThat(result.getSigAlg()).isNotEmpty();
		assertThat(result.getSignature()).isNotEmpty();
		assertThat(result.getBinding()).isEqualTo(Saml2MessageBinding.REDIRECT);
	}

	@Test
	public void createRedirectAuthenticationRequestWhenNotSignRequestThenNoSignatureIsPresent() {
		this.context = this.contextBuilder.relayState("Relay State Value")
				.relyingPartyRegistration(
						RelyingPartyRegistration.withRelyingPartyRegistration(this.relyingPartyRegistration)
								.providerDetails((c) -> c.signAuthNRequest(false)).build())
				.build();
		Saml2RedirectAuthenticationRequest result = this.factory.createRedirectAuthenticationRequest(this.context);
		assertThat(result.getSamlRequest()).isNotEmpty();
		assertThat(result.getRelayState()).isEqualTo("Relay State Value");
		assertThat(result.getSigAlg()).isNull();
		assertThat(result.getSignature()).isNull();
		assertThat(result.getBinding()).isEqualTo(Saml2MessageBinding.REDIRECT);
	}

	@Test
	public void createRedirectAuthenticationRequestWhenSignRequestThenSignatureIsPresent() {
		this.context = this.contextBuilder.relayState("Relay State Value")
				.relyingPartyRegistration(this.relyingPartyRegistration).build();
		Saml2RedirectAuthenticationRequest request = this.factory.createRedirectAuthenticationRequest(this.context);
		assertThat(request.getRelayState()).isEqualTo("Relay State Value");
		assertThat(request.getSigAlg()).isEqualTo(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		assertThat(request.getSignature()).isNotNull();
	}

	@Test
	public void createRedirectAuthenticationRequestWhenSignRequestThenCredentialIsRequired() {
		Saml2X509Credential credential = org.springframework.security.saml2.core.TestSaml2X509Credentials
				.relyingPartyVerifyingCredential();
		RelyingPartyRegistration registration = TestRelyingPartyRegistrations.noCredentials()
				.assertingPartyDetails((party) -> party.verificationX509Credentials((c) -> c.add(credential))).build();
		this.context = this.contextBuilder.relayState("Relay State Value").relyingPartyRegistration(registration)
				.build();
		assertThatExceptionOfType(Saml2Exception.class)
				.isThrownBy(() -> this.factory.createPostAuthenticationRequest(this.context));
	}

	@Test
	public void createPostAuthenticationRequestWhenNotSignRequestThenNoSignatureIsPresent() {
		this.context = this.contextBuilder.relayState("Relay State Value")
				.relyingPartyRegistration(
						RelyingPartyRegistration.withRelyingPartyRegistration(this.relyingPartyRegistration)
								.providerDetails((c) -> c.signAuthNRequest(false)).build())
				.build();
		Saml2PostAuthenticationRequest result = this.factory.createPostAuthenticationRequest(this.context);
		assertThat(result.getSamlRequest()).isNotEmpty();
		assertThat(result.getRelayState()).isEqualTo("Relay State Value");
		assertThat(result.getBinding()).isEqualTo(Saml2MessageBinding.POST);
		assertThat(new String(Saml2Utils.samlDecode(result.getSamlRequest()), StandardCharsets.UTF_8))
				.doesNotContain("ds:Signature");
	}

	@Test
	public void createPostAuthenticationRequestWhenSignRequestThenSignatureIsPresent() {
		this.context = this.contextBuilder.relayState("Relay State Value")
				.relyingPartyRegistration(
						RelyingPartyRegistration.withRelyingPartyRegistration(this.relyingPartyRegistration).build())
				.build();
		Saml2PostAuthenticationRequest result = this.factory.createPostAuthenticationRequest(this.context);
		assertThat(result.getSamlRequest()).isNotEmpty();
		assertThat(result.getRelayState()).isEqualTo("Relay State Value");
		assertThat(result.getBinding()).isEqualTo(Saml2MessageBinding.POST);
		assertThat(new String(Saml2Utils.samlDecode(result.getSamlRequest()), StandardCharsets.UTF_8))
				.contains("ds:Signature");
	}

	@Test
	public void createPostAuthenticationRequestWhenSignRequestThenCredentialIsRequired() {
		Saml2X509Credential credential = org.springframework.security.saml2.core.TestSaml2X509Credentials
				.relyingPartyVerifyingCredential();
		RelyingPartyRegistration registration = TestRelyingPartyRegistrations.noCredentials()
				.assertingPartyDetails((party) -> party.verificationX509Credentials((c) -> c.add(credential))).build();
		this.context = this.contextBuilder.relayState("Relay State Value").relyingPartyRegistration(registration)
				.build();
		assertThatExceptionOfType(Saml2Exception.class)
				.isThrownBy(() -> this.factory.createPostAuthenticationRequest(this.context));
	}

	@Test
	public void createAuthenticationRequestWhenDefaultThenReturnsPostBinding() {
		AuthnRequest authn = getAuthNRequest(Saml2MessageBinding.POST);
		Assert.assertEquals(SAMLConstants.SAML2_POST_BINDING_URI, authn.getProtocolBinding());
	}

	@Test
	public void createAuthenticationRequestWhenSetUriThenReturnsCorrectBinding() {
		this.factory.setProtocolBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
		AuthnRequest authn = getAuthNRequest(Saml2MessageBinding.POST);
		Assert.assertEquals(SAMLConstants.SAML2_REDIRECT_BINDING_URI, authn.getProtocolBinding());
	}

	@Test
	public void createAuthenticationRequestWhenSetUnsupportredUriThenThrowsIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.factory.setProtocolBinding("my-invalid-binding"))
				.withMessageContaining("my-invalid-binding");
	}

	@Test
	public void createPostAuthenticationRequestWhenAuthnRequestConsumerThenUses() {
		Converter<Saml2AuthenticationRequestContext, AuthnRequest> authenticationRequestContextConverter = mock(
				Converter.class);
		given(authenticationRequestContextConverter.convert(this.context))
				.willReturn(TestOpenSamlObjects.authnRequest());
		this.factory.setAuthenticationRequestContextConverter(authenticationRequestContextConverter);

		this.factory.createPostAuthenticationRequest(this.context);
		verify(authenticationRequestContextConverter).convert(this.context);
	}

	@Test
	public void createRedirectAuthenticationRequestWhenAuthnRequestConsumerThenUses() {
		Converter<Saml2AuthenticationRequestContext, AuthnRequest> authenticationRequestContextConverter = mock(
				Converter.class);
		given(authenticationRequestContextConverter.convert(this.context))
				.willReturn(TestOpenSamlObjects.authnRequest());
		this.factory.setAuthenticationRequestContextConverter(authenticationRequestContextConverter);

		this.factory.createRedirectAuthenticationRequest(this.context);
		verify(authenticationRequestContextConverter).convert(this.context);
	}

	@Test
	public void setAuthenticationRequestContextConverterWhenNullThenException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.factory.setAuthenticationRequestContextConverter(null));
		// @formatter:on
	}

	@Test
	public void createPostAuthenticationRequestWhenAssertionConsumerServiceBindingThenUses() {
		RelyingPartyRegistration relyingPartyRegistration = this.relyingPartyRegistrationBuilder
				.assertionConsumerServiceBinding(Saml2MessageBinding.REDIRECT).build();
		Saml2AuthenticationRequestContext context = this.contextBuilder
				.relyingPartyRegistration(relyingPartyRegistration).build();
		Saml2PostAuthenticationRequest request = this.factory.createPostAuthenticationRequest(context);
		String samlRequest = request.getSamlRequest();
		String inflated = new String(Saml2Utils.samlDecode(samlRequest));
		assertThat(inflated).contains("ProtocolBinding=\"" + SAMLConstants.SAML2_REDIRECT_BINDING_URI + "\"");
	}

	@Test
	public void createRedirectAuthenticationRequestWhenSHA1SignRequestThenSignatureIsPresent() {
		RelyingPartyRegistration relyingPartyRegistration = this.relyingPartyRegistrationBuilder
				.assertingPartyDetails(
						(a) -> a.signingAlgorithms((algs) -> algs.add(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1)))
				.build();
		Saml2AuthenticationRequestContext context = this.contextBuilder.relayState("Relay State Value")
				.relyingPartyRegistration(relyingPartyRegistration).build();
		Saml2RedirectAuthenticationRequest result = this.factory.createRedirectAuthenticationRequest(context);
		assertThat(result.getSamlRequest()).isNotEmpty();
		assertThat(result.getRelayState()).isEqualTo("Relay State Value");
		assertThat(result.getSigAlg()).isEqualTo(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
		assertThat(result.getSignature()).isNotNull();
		assertThat(result.getBinding()).isEqualTo(Saml2MessageBinding.REDIRECT);
	}

	private AuthnRequest getAuthNRequest(Saml2MessageBinding binding) {
		AbstractSaml2AuthenticationRequest result = (binding == Saml2MessageBinding.REDIRECT)
				? this.factory.createRedirectAuthenticationRequest(this.context)
				: this.factory.createPostAuthenticationRequest(this.context);
		String samlRequest = result.getSamlRequest();
		assertThat(samlRequest).isNotEmpty();
		if (result.getBinding() == Saml2MessageBinding.REDIRECT) {
			samlRequest = Saml2Utils.samlInflate(Saml2Utils.samlDecode(samlRequest));
		}
		else {
			samlRequest = new String(Saml2Utils.samlDecode(samlRequest), StandardCharsets.UTF_8);
		}
		try {
			Document document = XMLObjectProviderRegistrySupport.getParserPool()
					.parse(new ByteArrayInputStream(samlRequest.getBytes(StandardCharsets.UTF_8)));
			Element element = document.getDocumentElement();
			return (AuthnRequest) this.unmarshaller.unmarshall(element);
		}
		catch (Exception ex) {
			throw new Saml2Exception(ex);
		}
	}

}
