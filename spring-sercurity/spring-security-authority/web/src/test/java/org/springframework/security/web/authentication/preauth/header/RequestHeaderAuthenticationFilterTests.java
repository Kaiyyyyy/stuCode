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

package org.springframework.security.web.authentication.preauth.header;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Luke Taylor
 */
public class RequestHeaderAuthenticationFilterTests {

	@After
	@Before
	public void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void rejectsMissingHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		assertThatExceptionOfType(PreAuthenticatedCredentialsNotFoundException.class)
				.isThrownBy(() -> filter.doFilter(request, response, chain));
	}

	@Test
	public void defaultsToUsingSiteminderHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("SM_USER", "cat");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		filter.setAuthenticationManager(createAuthenticationManager());
		filter.doFilter(request, response, chain);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("cat");
		assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isEqualTo("N/A");
	}

	@Test
	public void alternativeHeaderNameIsSupported() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("myUsernameHeader", "wolfman");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		filter.setAuthenticationManager(createAuthenticationManager());
		filter.setPrincipalRequestHeader("myUsernameHeader");
		filter.doFilter(request, response, chain);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("wolfman");
	}

	@Test
	public void credentialsAreRetrievedIfHeaderNameIsSet() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		filter.setAuthenticationManager(createAuthenticationManager());
		filter.setCredentialsRequestHeader("myCredentialsHeader");
		request.addHeader("SM_USER", "cat");
		request.addHeader("myCredentialsHeader", "catspassword");
		filter.doFilter(request, response, chain);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isEqualTo("catspassword");
	}

	@Test
	public void userIsReauthenticatedIfPrincipalChangesAndCheckForPrincipalChangesIsSet() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		filter.setAuthenticationManager(createAuthenticationManager());
		filter.setCheckForPrincipalChanges(true);
		request.addHeader("SM_USER", "cat");
		filter.doFilter(request, response, new MockFilterChain());
		request = new MockHttpServletRequest();
		request.addHeader("SM_USER", "dog");
		filter.doFilter(request, response, new MockFilterChain());
		Authentication dog = SecurityContextHolder.getContext().getAuthentication();
		assertThat(dog).isNotNull();
		assertThat(dog.getName()).isEqualTo("dog");
		// Make sure authentication doesn't occur every time (i.e. if the header *doesn't
		// change)
		filter.setAuthenticationManager(mock(AuthenticationManager.class));
		filter.doFilter(request, response, new MockFilterChain());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(dog);
	}

	@Test
	public void missingHeaderCausesException() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		filter.setAuthenticationManager(createAuthenticationManager());
		assertThatExceptionOfType(PreAuthenticatedCredentialsNotFoundException.class)
				.isThrownBy(() -> filter.doFilter(request, response, chain));
	}

	@Test
	public void missingHeaderIsIgnoredIfExceptionIfHeaderMissingIsFalse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
		filter.setExceptionIfHeaderMissing(false);
		filter.setAuthenticationManager(createAuthenticationManager());
		filter.doFilter(request, response, chain);
	}

	/**
	 * Create an authentication manager which returns the passed in object.
	 */
	private AuthenticationManager createAuthenticationManager() {
		AuthenticationManager am = mock(AuthenticationManager.class);
		given(am.authenticate(any(Authentication.class)))
				.willAnswer((Answer<Authentication>) (invocation) -> (Authentication) invocation.getArguments()[0]);
		return am;
	}

}
