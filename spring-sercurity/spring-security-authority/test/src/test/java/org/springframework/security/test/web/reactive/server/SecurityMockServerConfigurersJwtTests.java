/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.security.test.web.reactive.server;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.TestJwts;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.reactive.result.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.security.web.server.context.SecurityContextServerWebExchangeWebFilter;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jérôme Wacongne &lt;ch4mp&#64;c4-soft.com&gt;
 * @author Josh Cummings
 * @since 5.2
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityMockServerConfigurersJwtTests extends AbstractMockServerConfigurersTests {

	@Mock
	GrantedAuthority authority1;

	@Mock
	GrantedAuthority authority2;

	WebTestClient client = WebTestClient.bindToController(this.securityContextController)
			.webFilter(new SecurityContextServerWebExchangeWebFilter())
			.argumentResolvers((resolvers) -> resolvers
					.addCustomResolver(new CurrentSecurityContextArgumentResolver(new ReactiveAdapterRegistry())))
			.apply(SecurityMockServerConfigurers.springSecurity()).configureClient()
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build();

	@Test
	public void mockJwtWhenUsingDefaultsTheCreatesJwtAuthentication() {
		this.client.mutateWith(SecurityMockServerConfigurers.mockJwt()).get().exchange().expectStatus().isOk();
		SecurityContext context = this.securityContextController.removeSecurityContext();
		assertThat(context.getAuthentication()).isInstanceOf(JwtAuthenticationToken.class);
		JwtAuthenticationToken token = (JwtAuthenticationToken) context.getAuthentication();
		assertThat(token.getAuthorities()).isNotEmpty();
		assertThat(token.getToken()).isNotNull();
		assertThat(token.getToken().getSubject()).isEqualTo("user");
		assertThat(token.getToken().getHeaders().get("alg")).isEqualTo("none");
	}

	@Test
	public void mockJwtWhenProvidingBuilderConsumerThenProducesJwtAuthentication() {
		String name = new String("user");
		this.client.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt((jwt) -> jwt.subject(name))).get().exchange()
				.expectStatus().isOk();
		SecurityContext context = this.securityContextController.removeSecurityContext();
		assertThat(context.getAuthentication()).isInstanceOf(JwtAuthenticationToken.class);
		JwtAuthenticationToken token = (JwtAuthenticationToken) context.getAuthentication();
		assertThat(token.getToken().getSubject()).isSameAs(name);
	}

	@Test
	public void mockJwtWhenProvidingCustomAuthoritiesThenProducesJwtAuthentication() {
		this.client.mutateWith(SecurityMockServerConfigurers.mockJwt()
				.jwt((jwt) -> jwt.claim("scope", "ignored authorities")).authorities(this.authority1, this.authority2))
				.get().exchange().expectStatus().isOk();
		SecurityContext context = this.securityContextController.removeSecurityContext();
		assertThat((List<GrantedAuthority>) context.getAuthentication().getAuthorities()).containsOnly(this.authority1,
				this.authority2);
	}

	@Test
	public void mockJwtWhenProvidingScopedAuthoritiesThenProducesJwtAuthentication() {
		this.client
				.mutateWith(
						SecurityMockServerConfigurers.mockJwt().jwt((jwt) -> jwt.claim("scope", "scoped authorities")))
				.get().exchange().expectStatus().isOk();
		SecurityContext context = this.securityContextController.removeSecurityContext();
		assertThat((List<GrantedAuthority>) context.getAuthentication().getAuthorities()).containsOnly(
				new SimpleGrantedAuthority("SCOPE_scoped"), new SimpleGrantedAuthority("SCOPE_authorities"));
	}

	@Test
	public void mockJwtWhenProvidingGrantedAuthoritiesThenProducesJwtAuthentication() {
		this.client
				.mutateWith(
						SecurityMockServerConfigurers.mockJwt().jwt((jwt) -> jwt.claim("scope", "ignored authorities"))
								.authorities((jwt) -> Arrays.asList(this.authority1)))
				.get().exchange().expectStatus().isOk();
		SecurityContext context = this.securityContextController.removeSecurityContext();
		assertThat((List<GrantedAuthority>) context.getAuthentication().getAuthorities()).containsOnly(this.authority1);
	}

	@Test
	public void mockJwtWhenProvidingPreparedJwtThenProducesJwtAuthentication() {
		Jwt originalToken = TestJwts.jwt().header("header1", "value1").subject("some_user").build();
		this.client.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(originalToken)).get().exchange()
				.expectStatus().isOk();
		SecurityContext context = this.securityContextController.removeSecurityContext();
		assertThat(context.getAuthentication()).isInstanceOf(JwtAuthenticationToken.class);
		JwtAuthenticationToken retrievedToken = (JwtAuthenticationToken) context.getAuthentication();
		assertThat(retrievedToken.getToken().getSubject()).isEqualTo("some_user");
		assertThat(retrievedToken.getToken().getTokenValue()).isEqualTo("token");
		assertThat(retrievedToken.getToken().getHeaders().get("header1")).isEqualTo("value1");
	}

}
