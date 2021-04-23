/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.security.config.method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.BusinessService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.ConfigTestUtils;
import org.springframework.security.config.util.InMemoryXmlApplicationContext;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Luke Taylor
 */
public class Jsr250AnnotationDrivenBeanDefinitionParserTests {

	private InMemoryXmlApplicationContext appContext;

	private BusinessService target;

	@Before
	public void loadContext() {
		// @formatter:off
		this.appContext = new InMemoryXmlApplicationContext(
				"<b:bean id='target' class='org.springframework.security.access.annotation.Jsr250BusinessServiceImpl'/>"
						+ "<global-method-security jsr250-annotations='enabled'/>"
						+ ConfigTestUtils.AUTH_PROVIDER_XML);
		// @formatter:on
		this.target = (BusinessService) this.appContext.getBean("target");
	}

	@After
	public void closeAppContext() {
		if (this.appContext != null) {
			this.appContext.close();
		}
		SecurityContextHolder.clearContext();
	}

	@Test
	public void targetShouldPreventProtectedMethodInvocationWithNoContext() {
		assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
				.isThrownBy(() -> this.target.someUserMethod1());
	}

	@Test
	public void permitAllShouldBeDefaultAttribute() {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(token);
		this.target.someOther(0);
	}

	@Test
	public void targetShouldAllowProtectedMethodInvocationWithCorrectRole() {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(token);
		this.target.someUserMethod1();
	}

	@Test
	public void targetShouldPreventProtectedMethodInvocationWithIncorrectRole() {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
				AuthorityUtils.createAuthorityList("ROLE_SOMEOTHERROLE"));
		SecurityContextHolder.getContext().setAuthentication(token);
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.target::someAdminMethod);
	}

	@Test
	public void hasAnyRoleAddsDefaultPrefix() {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(token);
		this.target.rolesAllowedUser();
	}

}
