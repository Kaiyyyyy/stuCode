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

package org.springframework.security.oauth2.client.web.method.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.PasswordOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OAuth2AuthorizedClientArgumentResolver}.
 *
 * @author Joe Grandja
 */
public class OAuth2AuthorizedClientArgumentResolverTests {

	private TestingAuthenticationToken authentication;

	private String principalName = "principal-1";

	private ClientRegistration registration1;

	private ClientRegistration registration2;

	private ClientRegistration registration3;

	private ClientRegistrationRepository clientRegistrationRepository;

	private OAuth2AuthorizedClient authorizedClient1;

	private OAuth2AuthorizedClient authorizedClient2;

	private OAuth2AuthorizedClientRepository authorizedClientRepository;

	private OAuth2AuthorizedClientArgumentResolver argumentResolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setup() {
		this.authentication = new TestingAuthenticationToken(this.principalName, "password");
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(this.authentication);
		SecurityContextHolder.setContext(securityContext);
		// @formatter:off
		this.registration1 = ClientRegistration.withRegistrationId("client1")
				.clientId("client-1")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.scope("user")
				.authorizationUri("https://provider.com/oauth2/authorize")
				.tokenUri("https://provider.com/oauth2/token")
				.userInfoUri("https://provider.com/oauth2/user")
				.userNameAttributeName("id")
				.clientName("client-1")
				.build();
		this.registration2 = ClientRegistration.withRegistrationId("client2")
				.clientId("client-2")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("read", "write")
				.tokenUri("https://provider.com/oauth2/token")
				.build();
		this.registration3 = TestClientRegistrations.password()
				.registrationId("client3")
				.build();
		// @formatter:on
		this.clientRegistrationRepository = new InMemoryClientRegistrationRepository(this.registration1,
				this.registration2, this.registration3);
		this.authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.authorizationCode().refreshToken().clientCredentials().build();
		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
				this.clientRegistrationRepository, this.authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		this.argumentResolver = new OAuth2AuthorizedClientArgumentResolver(authorizedClientManager);
		this.authorizedClient1 = new OAuth2AuthorizedClient(this.registration1, this.principalName,
				mock(OAuth2AccessToken.class));
		given(this.authorizedClientRepository.loadAuthorizedClient(eq(this.registration1.getRegistrationId()),
				any(Authentication.class), any(HttpServletRequest.class))).willReturn(this.authorizedClient1);
		this.authorizedClient2 = new OAuth2AuthorizedClient(this.registration2, this.principalName,
				mock(OAuth2AccessToken.class));
		given(this.authorizedClientRepository.loadAuthorizedClient(eq(this.registration2.getRegistrationId()),
				any(Authentication.class), any(HttpServletRequest.class))).willReturn(this.authorizedClient2);
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@After
	public void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void constructorWhenClientRegistrationRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new OAuth2AuthorizedClientArgumentResolver(null, this.authorizedClientRepository));
	}

	@Test
	public void constructorWhenOAuth2AuthorizedClientRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new OAuth2AuthorizedClientArgumentResolver(this.clientRegistrationRepository, null));
	}

	@Test
	public void constructorWhenAuthorizedClientManagerIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new OAuth2AuthorizedClientArgumentResolver(null));
	}

	@Test
	public void setClientCredentialsTokenResponseClientWhenClientIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.argumentResolver.setClientCredentialsTokenResponseClient(null))
				.withMessage("clientCredentialsTokenResponseClient cannot be null");
	}

	@Test
	public void setClientCredentialsTokenResponseClientWhenNotDefaultAuthorizedClientManagerThenThrowIllegalStateException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.argumentResolver
						.setClientCredentialsTokenResponseClient(new DefaultClientCredentialsTokenResponseClient()))
				.withMessage("The client cannot be set when the constructor used is "
						+ "\"OAuth2AuthorizedClientArgumentResolver(OAuth2AuthorizedClientManager)\". "
						+ "Instead, use the constructor \"OAuth2AuthorizedClientArgumentResolver(ClientRegistrationRepository, "
						+ "OAuth2AuthorizedClientRepository)\".");
	}

	@Test
	public void supportsParameterWhenParameterTypeOAuth2AuthorizedClientThenTrue() {
		MethodParameter methodParameter = this.getMethodParameter("paramTypeAuthorizedClient",
				OAuth2AuthorizedClient.class);
		assertThat(this.argumentResolver.supportsParameter(methodParameter)).isTrue();
	}

	@Test
	public void supportsParameterWhenParameterTypeOAuth2AuthorizedClientWithoutAnnotationThenFalse() {
		MethodParameter methodParameter = this.getMethodParameter("paramTypeAuthorizedClientWithoutAnnotation",
				OAuth2AuthorizedClient.class);
		assertThat(this.argumentResolver.supportsParameter(methodParameter)).isFalse();
	}

	@Test
	public void supportsParameterWhenParameterTypeUnsupportedThenFalse() {
		MethodParameter methodParameter = this.getMethodParameter("paramTypeUnsupported", String.class);
		assertThat(this.argumentResolver.supportsParameter(methodParameter)).isFalse();
	}

	@Test
	public void supportsParameterWhenParameterTypeUnsupportedWithoutAnnotationThenFalse() {
		MethodParameter methodParameter = this.getMethodParameter("paramTypeUnsupportedWithoutAnnotation",
				String.class);
		assertThat(this.argumentResolver.supportsParameter(methodParameter)).isFalse();
	}

	@Test
	public void resolveArgumentWhenRegistrationIdEmptyAndNotOAuth2AuthenticationThenThrowIllegalArgumentException() {
		MethodParameter methodParameter = this.getMethodParameter("registrationIdEmpty", OAuth2AuthorizedClient.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.argumentResolver.resolveArgument(methodParameter, null, null, null))
				.withMessage("Unable to resolve the Client Registration Identifier. It must be provided via "
						+ "@RegisteredOAuth2AuthorizedClient(\"client1\") or "
						+ "@RegisteredOAuth2AuthorizedClient(registrationId = \"client1\").");
	}

	@Test
	public void resolveArgumentWhenRegistrationIdEmptyAndOAuth2AuthenticationThenResolves() throws Exception {
		OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
		given(authentication.getAuthorizedClientRegistrationId()).willReturn("client1");
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);
		MethodParameter methodParameter = this.getMethodParameter("registrationIdEmpty", OAuth2AuthorizedClient.class);
		assertThat(this.argumentResolver.resolveArgument(methodParameter, null,
				new ServletWebRequest(this.request, this.response), null)).isSameAs(this.authorizedClient1);
	}

	@Test
	public void resolveArgumentWhenAuthorizedClientFoundThenResolves() throws Exception {
		MethodParameter methodParameter = this.getMethodParameter("paramTypeAuthorizedClient",
				OAuth2AuthorizedClient.class);
		assertThat(this.argumentResolver.resolveArgument(methodParameter, null,
				new ServletWebRequest(this.request, this.response), null)).isSameAs(this.authorizedClient1);
	}

	@Test
	public void resolveArgumentWhenRegistrationIdInvalidThenThrowIllegalArgumentException() {
		MethodParameter methodParameter = this.getMethodParameter("registrationIdInvalid",
				OAuth2AuthorizedClient.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.argumentResolver.resolveArgument(methodParameter, null,
						new ServletWebRequest(this.request, this.response), null))
				.withMessage("Could not find ClientRegistration with id 'invalid'");
	}

	@Test
	public void resolveArgumentWhenAuthorizedClientNotFoundForAuthorizationCodeClientThenThrowClientAuthorizationRequiredException() {
		given(this.authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any(HttpServletRequest.class)))
				.willReturn(null);
		MethodParameter methodParameter = this.getMethodParameter("paramTypeAuthorizedClient",
				OAuth2AuthorizedClient.class);
		assertThatExceptionOfType(ClientAuthorizationRequiredException.class).isThrownBy(() -> this.argumentResolver
				.resolveArgument(methodParameter, null, new ServletWebRequest(this.request, this.response), null));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resolveArgumentWhenAuthorizedClientNotFoundForClientCredentialsClientThenResolvesFromTokenResponseClient()
			throws Exception {
		OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient = mock(
				OAuth2AccessTokenResponseClient.class);
		ClientCredentialsOAuth2AuthorizedClientProvider clientCredentialsAuthorizedClientProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();
		clientCredentialsAuthorizedClientProvider.setAccessTokenResponseClient(clientCredentialsTokenResponseClient);
		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
				this.clientRegistrationRepository, this.authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(clientCredentialsAuthorizedClientProvider);
		this.argumentResolver = new OAuth2AuthorizedClientArgumentResolver(authorizedClientManager);
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("access-token-1234")
				.tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).build();
		given(clientCredentialsTokenResponseClient.getTokenResponse(any())).willReturn(accessTokenResponse);
		given(this.authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any(HttpServletRequest.class)))
				.willReturn(null);
		MethodParameter methodParameter = this.getMethodParameter("clientCredentialsClient",
				OAuth2AuthorizedClient.class);
		OAuth2AuthorizedClient authorizedClient = (OAuth2AuthorizedClient) this.argumentResolver
				.resolveArgument(methodParameter, null, new ServletWebRequest(this.request, this.response), null);
		assertThat(authorizedClient).isNotNull();
		assertThat(authorizedClient.getClientRegistration()).isSameAs(this.registration2);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo(this.principalName);
		assertThat(authorizedClient.getAccessToken()).isSameAs(accessTokenResponse.getAccessToken());
		verify(this.authorizedClientRepository).saveAuthorizedClient(eq(authorizedClient), eq(this.authentication),
				any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resolveArgumentWhenAuthorizedClientNotFoundForPasswordClientThenResolvesFromTokenResponseClient()
			throws Exception {
		OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> passwordTokenResponseClient = mock(
				OAuth2AccessTokenResponseClient.class);
		PasswordOAuth2AuthorizedClientProvider passwordAuthorizedClientProvider = new PasswordOAuth2AuthorizedClientProvider();
		passwordAuthorizedClientProvider.setAccessTokenResponseClient(passwordTokenResponseClient);
		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
				this.clientRegistrationRepository, this.authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(passwordAuthorizedClientProvider);
		// Set custom contextAttributesMapper
		authorizedClientManager.setContextAttributesMapper((authorizeRequest) -> {
			Map<String, Object> contextAttributes = new HashMap<>();
			HttpServletRequest servletRequest = authorizeRequest.getAttribute(HttpServletRequest.class.getName());
			String username = servletRequest.getParameter(OAuth2ParameterNames.USERNAME);
			String password = servletRequest.getParameter(OAuth2ParameterNames.PASSWORD);
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
				contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
			}
			return contextAttributes;
		});
		this.argumentResolver = new OAuth2AuthorizedClientArgumentResolver(authorizedClientManager);
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("access-token-1234")
				.tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).build();
		given(passwordTokenResponseClient.getTokenResponse(any())).willReturn(accessTokenResponse);
		given(this.authorizedClientRepository.loadAuthorizedClient(anyString(), any(), any(HttpServletRequest.class)))
				.willReturn(null);
		MethodParameter methodParameter = this.getMethodParameter("passwordClient", OAuth2AuthorizedClient.class);
		this.request.setParameter(OAuth2ParameterNames.USERNAME, "username");
		this.request.setParameter(OAuth2ParameterNames.PASSWORD, "password");
		OAuth2AuthorizedClient authorizedClient = (OAuth2AuthorizedClient) this.argumentResolver
				.resolveArgument(methodParameter, null, new ServletWebRequest(this.request, this.response), null);
		assertThat(authorizedClient).isNotNull();
		assertThat(authorizedClient.getClientRegistration()).isSameAs(this.registration3);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo(this.principalName);
		assertThat(authorizedClient.getAccessToken()).isSameAs(accessTokenResponse.getAccessToken());
		verify(this.authorizedClientRepository).saveAuthorizedClient(eq(authorizedClient), eq(this.authentication),
				any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	private MethodParameter getMethodParameter(String methodName, Class<?>... paramTypes) {
		Method method = ReflectionUtils.findMethod(TestController.class, methodName, paramTypes);
		return new MethodParameter(method, 0);
	}

	static class TestController {

		void paramTypeAuthorizedClient(
				@RegisteredOAuth2AuthorizedClient("client1") OAuth2AuthorizedClient authorizedClient) {
		}

		void paramTypeAuthorizedClientWithoutAnnotation(OAuth2AuthorizedClient authorizedClient) {
		}

		void paramTypeUnsupported(@RegisteredOAuth2AuthorizedClient("client1") String param) {
		}

		void paramTypeUnsupportedWithoutAnnotation(String param) {
		}

		void registrationIdEmpty(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient) {
		}

		void registrationIdInvalid(
				@RegisteredOAuth2AuthorizedClient("invalid") OAuth2AuthorizedClient authorizedClient) {
		}

		void clientCredentialsClient(
				@RegisteredOAuth2AuthorizedClient("client2") OAuth2AuthorizedClient authorizedClient) {
		}

		void passwordClient(@RegisteredOAuth2AuthorizedClient("client3") OAuth2AuthorizedClient authorizedClient) {
		}

	}

}
