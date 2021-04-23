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

package org.springframework.security.config.annotation.authentication;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.test.SpringTestRule;
import org.springframework.security.core.userdetails.PasswordEncodedUser;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

/**
 * @author Rob Winch
 */
public class NamespaceAuthenticationManagerTests {

	@Rule
	public final SpringTestRule spring = new SpringTestRule();

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void authenticationMangerWhenDefaultThenEraseCredentialsIsTrue() throws Exception {
		this.spring.register(EraseCredentialsTrueDefaultConfig.class).autowire();
		SecurityMockMvcResultMatchers.AuthenticatedMatcher nullCredentials = authenticated()
				.withAuthentication((a) -> assertThat(a.getCredentials()).isNull());
		this.mockMvc.perform(formLogin()).andExpect(nullCredentials);
		this.mockMvc.perform(formLogin()).andExpect(nullCredentials);
		// no exception due to username being cleared out
	}

	@Test
	public void authenticationMangerWhenEraseCredentialsIsFalseThenCredentialsNotNull() throws Exception {
		this.spring.register(EraseCredentialsFalseConfig.class).autowire();
		SecurityMockMvcResultMatchers.AuthenticatedMatcher notNullCredentials = authenticated()
				.withAuthentication((a) -> assertThat(a.getCredentials()).isNotNull());
		this.mockMvc.perform(formLogin()).andExpect(notNullCredentials);
		this.mockMvc.perform(formLogin()).andExpect(notNullCredentials);
		// no exception due to username being cleared out
	}

	@Test
	// SEC-2533
	public void authenticationManagerWhenGlobalAndEraseCredentialsIsFalseThenCredentialsNotNull() throws Exception {
		this.spring.register(GlobalEraseCredentialsFalseConfig.class).autowire();
		SecurityMockMvcResultMatchers.AuthenticatedMatcher notNullCredentials = authenticated()
				.withAuthentication((a) -> assertThat(a.getCredentials()).isNotNull());
		this.mockMvc.perform(formLogin()).andExpect(notNullCredentials);
	}

	@EnableWebSecurity
	static class EraseCredentialsTrueDefaultConfig extends WebSecurityConfigurerAdapter {

		@Autowired
		void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			// @formatter:off
			auth
				.inMemoryAuthentication()
					.withUser(PasswordEncodedUser.user());
			// @formatter:on
		}

	}

	@EnableWebSecurity
	static class EraseCredentialsFalseConfig extends WebSecurityConfigurerAdapter {

		@Override
		public void configure(AuthenticationManagerBuilder auth) throws Exception {
			// @formatter:off
			auth
				.eraseCredentials(false)
				.inMemoryAuthentication()
				.withUser(PasswordEncodedUser.user());
			// @formatter:on
		}

	}

	@EnableWebSecurity
	static class GlobalEraseCredentialsFalseConfig extends WebSecurityConfigurerAdapter {

		@Autowired
		void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			// @formatter:off
			auth
				.eraseCredentials(false)
				.inMemoryAuthentication()
				.withUser(PasswordEncodedUser.user());
			// @formatter:on
		}

	}

}
