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

package org.springframework.security.oauth2.client.authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Winch
 * @since 5.1
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuth2LoginReactiveAuthenticationManagerTests {

	@Mock
	private ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService;

	@Mock
	private ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

	@Mock
	private ReactiveOAuth2AuthorizedClientService authorizedClientService;

	private ClientRegistration.Builder registration = TestClientRegistrations.clientRegistration();

	OAuth2AuthorizationResponse.Builder authorizationResponseBldr = OAuth2AuthorizationResponse.success("code")
			.state("state");

	private OAuth2LoginReactiveAuthenticationManager manager;

	@Before
	public void setup() {
		this.manager = new OAuth2LoginReactiveAuthenticationManager(this.accessTokenResponseClient, this.userService);
	}

	@Test
	public void constructorWhenNullAccessTokenResponseClientThenIllegalArgumentException() {
		this.accessTokenResponseClient = null;
		assertThatIllegalArgumentException().isThrownBy(
				() -> new OAuth2LoginReactiveAuthenticationManager(this.accessTokenResponseClient, this.userService));
	}

	@Test
	public void constructorWhenNullUserServiceThenIllegalArgumentException() {
		this.userService = null;
		assertThatIllegalArgumentException().isThrownBy(
				() -> new OAuth2LoginReactiveAuthenticationManager(this.accessTokenResponseClient, this.userService));
	}

	@Test
	public void setAuthoritiesMapperWhenAuthoritiesMapperIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.manager.setAuthoritiesMapper(null));
	}

	@Test
	public void authenticateWhenNoSubscriptionThenDoesNothing() {
		// we didn't do anything because it should cause a ClassCastException (as verified
		// below)
		TestingAuthenticationToken token = new TestingAuthenticationToken("a", "b");
		this.manager.authenticate(token);
		assertThatExceptionOfType(Throwable.class).isThrownBy(() -> this.manager.authenticate(token).block());
	}

	@Test
	@Ignore
	public void authenticationWhenOidcThenEmpty() {
		this.registration.scope("openid");
		assertThat(this.manager.authenticate(loginToken()).block()).isNull();
	}

	@Test
	public void authenticationWhenErrorThenOAuth2AuthenticationException() {
		// @formatter:off
		this.authorizationResponseBldr = OAuth2AuthorizationResponse
				.error("error")
				.state("state");
		// @formatter:on
		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> this.manager.authenticate(loginToken()).block());
	}

	@Test
	public void authenticationWhenStateDoesNotMatchThenOAuth2AuthenticationException() {
		this.authorizationResponseBldr.state("notmatch");
		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> this.manager.authenticate(loginToken()).block());
	}

	@Test
	public void authenticationWhenOAuth2UserNotFoundThenEmpty() {
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("foo")
				.tokenType(OAuth2AccessToken.TokenType.BEARER).build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(Mono.just(accessTokenResponse));
		given(this.userService.loadUser(any())).willReturn(Mono.empty());
		assertThat(this.manager.authenticate(loginToken()).block()).isNull();
	}

	@Test
	public void authenticationWhenOAuth2UserFoundThenSuccess() {
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("foo")
				.tokenType(OAuth2AccessToken.TokenType.BEARER).build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(Mono.just(accessTokenResponse));
		DefaultOAuth2User user = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("ROLE_USER"),
				Collections.singletonMap("user", "rob"), "user");
		given(this.userService.loadUser(any())).willReturn(Mono.just(user));
		OAuth2LoginAuthenticationToken result = (OAuth2LoginAuthenticationToken) this.manager.authenticate(loginToken())
				.block();
		assertThat(result.getPrincipal()).isEqualTo(user);
		assertThat(result.getAuthorities()).containsOnlyElementsOf(user.getAuthorities());
		assertThat(result.isAuthenticated()).isTrue();
	}

	// gh-5368
	@Test
	public void authenticateWhenTokenSuccessResponseThenAdditionalParametersAddedToUserRequest() {
		Map<String, Object> additionalParameters = new HashMap<>();
		additionalParameters.put("param1", "value1");
		additionalParameters.put("param2", "value2");
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("foo")
				.tokenType(OAuth2AccessToken.TokenType.BEARER).additionalParameters(additionalParameters).build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(Mono.just(accessTokenResponse));
		DefaultOAuth2User user = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("ROLE_USER"),
				Collections.singletonMap("user", "rob"), "user");
		ArgumentCaptor<OAuth2UserRequest> userRequestArgCaptor = ArgumentCaptor.forClass(OAuth2UserRequest.class);
		given(this.userService.loadUser(userRequestArgCaptor.capture())).willReturn(Mono.just(user));
		this.manager.authenticate(loginToken()).block();
		assertThat(userRequestArgCaptor.getValue().getAdditionalParameters())
				.containsAllEntriesOf(accessTokenResponse.getAdditionalParameters());
	}

	@Test
	public void authenticateWhenAuthoritiesMapperSetThenReturnMappedAuthorities() {
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("foo")
				.tokenType(OAuth2AccessToken.TokenType.BEARER).build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(Mono.just(accessTokenResponse));
		DefaultOAuth2User user = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("ROLE_USER"),
				Collections.singletonMap("user", "rob"), "user");
		given(this.userService.loadUser(any())).willReturn(Mono.just(user));
		List<GrantedAuthority> mappedAuthorities = AuthorityUtils.createAuthorityList("ROLE_OAUTH_USER");
		GrantedAuthoritiesMapper authoritiesMapper = mock(GrantedAuthoritiesMapper.class);
		given(authoritiesMapper.mapAuthorities(anyCollection()))
				.willAnswer((Answer<List<GrantedAuthority>>) (invocation) -> mappedAuthorities);
		this.manager.setAuthoritiesMapper(authoritiesMapper);
		OAuth2LoginAuthenticationToken result = (OAuth2LoginAuthenticationToken) this.manager.authenticate(loginToken())
				.block();
		assertThat(result.getAuthorities()).isEqualTo(mappedAuthorities);
	}

	private OAuth2AuthorizationCodeAuthenticationToken loginToken() {
		ClientRegistration clientRegistration = this.registration.build();
		OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode().state("state")
				.clientId(clientRegistration.getClientId())
				.authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
				.redirectUri(clientRegistration.getRedirectUri()).scopes(clientRegistration.getScopes()).build();
		OAuth2AuthorizationResponse authorizationResponse = this.authorizationResponseBldr
				.redirectUri(clientRegistration.getRedirectUri()).build();
		OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(authorizationRequest,
				authorizationResponse);
		return new OAuth2AuthorizationCodeAuthenticationToken(clientRegistration, authorizationExchange);
	}

}
