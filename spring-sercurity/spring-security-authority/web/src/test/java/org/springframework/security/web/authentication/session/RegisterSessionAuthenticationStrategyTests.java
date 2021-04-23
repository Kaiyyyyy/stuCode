/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.security.web.authentication.session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;

/**
 * @author Rob Winch
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterSessionAuthenticationStrategyTests {

	@Mock
	private SessionRegistry registry;

	private RegisterSessionAuthenticationStrategy authenticationStrategy;

	private Authentication authentication;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setup() {
		this.authenticationStrategy = new RegisterSessionAuthenticationStrategy(this.registry);
		this.authentication = new TestingAuthenticationToken("user", "password", "ROLE_USER");
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void constructorNullRegistry() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RegisterSessionAuthenticationStrategy(null));
	}

	@Test
	public void onAuthenticationRegistersSession() {
		this.authenticationStrategy.onAuthentication(this.authentication, this.request, this.response);
		verify(this.registry).registerNewSession(this.request.getSession().getId(), this.authentication.getPrincipal());
	}

}
