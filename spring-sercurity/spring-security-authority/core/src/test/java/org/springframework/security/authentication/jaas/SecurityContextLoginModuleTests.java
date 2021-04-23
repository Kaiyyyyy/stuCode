/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.authentication.jaas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests SecurityContextLoginModule
 *
 * @author Ray Krueger
 */
public class SecurityContextLoginModuleTests {

	private SecurityContextLoginModule module = null;

	private Subject subject = new Subject(false, new HashSet<>(), new HashSet<>(), new HashSet<>());

	private UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("principal",
			"credentials");

	@Before
	public void setUp() {
		this.module = new SecurityContextLoginModule();
		this.module.initialize(this.subject, null, null, null);
		SecurityContextHolder.clearContext();
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
		this.module = null;
	}

	@Test
	public void testAbort() throws Exception {
		assertThat(this.module.abort()).as("Should return false, no auth is set").isFalse();
		SecurityContextHolder.getContext().setAuthentication(this.auth);
		this.module.login();
		this.module.commit();
		assertThat(this.module.abort()).isTrue();
	}

	@Test
	public void testLoginException() {
		assertThatExceptionOfType(LoginException.class).isThrownBy(this.module::login);
	}

	@Test
	public void testLoginSuccess() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(this.auth);
		assertThat(this.module.login()).as("Login should succeed, there is an authentication set").isTrue();
		assertThat(this.module.commit()).withFailMessage("The authentication is not null, this should return true")
				.isTrue();
		assertThat(this.subject.getPrincipals().contains(this.auth))
				.withFailMessage("Principals should contain the authentication").isTrue();
	}

	@Test
	public void testLogout() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(this.auth);
		this.module.login();
		assertThat(this.module.logout()).as("Should return true as it succeeds").isTrue();
		assertThat(this.module.getAuthentication()).as("Authentication should be null").isNull();
		assertThat(this.subject.getPrincipals().contains(this.auth))
				.withFailMessage("Principals should not contain the authentication after logout").isFalse();
	}

	@Test
	public void testNullAuthenticationInSecurityContext() {
		SecurityContextHolder.getContext().setAuthentication(null);
		assertThatExceptionOfType(Exception.class).isThrownBy(this.module::login);
	}

	@Test
	public void testNullAuthenticationInSecurityContextIgnored() throws Exception {
		this.module = new SecurityContextLoginModule();
		Map<String, String> options = new HashMap<>();
		options.put("ignoreMissingAuthentication", "true");
		this.module.initialize(this.subject, null, null, options);
		SecurityContextHolder.getContext().setAuthentication(null);
		assertThat(this.module.login()).as("Should return false and ask to be ignored").isFalse();
	}

	@Test
	public void testNullLogout() throws Exception {
		assertThat(this.module.logout()).isFalse();
	}

}
