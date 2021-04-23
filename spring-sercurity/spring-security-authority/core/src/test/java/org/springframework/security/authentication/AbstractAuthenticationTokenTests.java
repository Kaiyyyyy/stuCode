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

package org.springframework.security.authentication;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link AbstractAuthenticationToken}.
 *
 * @author Ben Alex
 */
public class AbstractAuthenticationTokenTests {

	private List<GrantedAuthority> authorities = null;

	@Before
	public final void setUp() {
		this.authorities = AuthorityUtils.createAuthorityList("ROLE_ONE", "ROLE_TWO");
	}

	@Test
	public void testAuthoritiesAreImmutable() {
		MockAuthenticationImpl token = new MockAuthenticationImpl("Test", "Password", this.authorities);
		List<GrantedAuthority> gotAuthorities = (List<GrantedAuthority>) token.getAuthorities();
		assertThat(gotAuthorities).isNotSameAs(this.authorities);
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> gotAuthorities.set(0, new SimpleGrantedAuthority("ROLE_SUPER_USER")));
	}

	@Test
	public void testGetters() {
		MockAuthenticationImpl token = new MockAuthenticationImpl("Test", "Password", this.authorities);
		assertThat(token.getPrincipal()).isEqualTo("Test");
		assertThat(token.getCredentials()).isEqualTo("Password");
		assertThat(token.getName()).isEqualTo("Test");
	}

	@Test
	public void testHashCode() {
		MockAuthenticationImpl token1 = new MockAuthenticationImpl("Test", "Password", this.authorities);
		MockAuthenticationImpl token2 = new MockAuthenticationImpl("Test", "Password", this.authorities);
		MockAuthenticationImpl token3 = new MockAuthenticationImpl(null, null, AuthorityUtils.NO_AUTHORITIES);
		assertThat(token2.hashCode()).isEqualTo(token1.hashCode());
		assertThat(token1.hashCode() != token3.hashCode()).isTrue();
		token2.setAuthenticated(true);
		assertThat(token1.hashCode() != token2.hashCode()).isTrue();
	}

	@Test
	public void testObjectsEquals() {
		MockAuthenticationImpl token1 = new MockAuthenticationImpl("Test", "Password", this.authorities);
		MockAuthenticationImpl token2 = new MockAuthenticationImpl("Test", "Password", this.authorities);
		assertThat(token2).isEqualTo(token1);
		MockAuthenticationImpl token3 = new MockAuthenticationImpl("Test", "Password_Changed", this.authorities);
		assertThat(!token1.equals(token3)).isTrue();
		MockAuthenticationImpl token4 = new MockAuthenticationImpl("Test_Changed", "Password", this.authorities);
		assertThat(!token1.equals(token4)).isTrue();
		MockAuthenticationImpl token5 = new MockAuthenticationImpl("Test", "Password",
				AuthorityUtils.createAuthorityList("ROLE_ONE", "ROLE_TWO_CHANGED"));
		assertThat(!token1.equals(token5)).isTrue();
		MockAuthenticationImpl token6 = new MockAuthenticationImpl("Test", "Password",
				AuthorityUtils.createAuthorityList("ROLE_ONE"));
		assertThat(!token1.equals(token6)).isTrue();
		MockAuthenticationImpl token7 = new MockAuthenticationImpl("Test", "Password", null);
		assertThat(!token1.equals(token7)).isTrue();
		assertThat(!token7.equals(token1)).isTrue();
		assertThat(!token1.equals(100)).isTrue();
	}

	@Test
	public void testSetAuthenticated() {
		MockAuthenticationImpl token = new MockAuthenticationImpl("Test", "Password", this.authorities);
		assertThat(!token.isAuthenticated()).isTrue();
		token.setAuthenticated(true);
		assertThat(token.isAuthenticated()).isTrue();
	}

	@Test
	public void testToStringWithAuthorities() {
		MockAuthenticationImpl token = new MockAuthenticationImpl("Test", "Password", this.authorities);
		assertThat(token.toString().lastIndexOf("ROLE_TWO") != -1).isTrue();
	}

	@Test
	public void testToStringWithNullAuthorities() {
		MockAuthenticationImpl token = new MockAuthenticationImpl("Test", "Password", null);
		assertThat(token.toString().lastIndexOf("Granted Authorities=[]") != -1).isTrue();
	}

	@Test
	public void testGetNameWhenPrincipalIsAuthenticatedPrincipal() {
		String principalName = "test";
		AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
		given(principal.getName()).willReturn(principalName);
		MockAuthenticationImpl token = new MockAuthenticationImpl(principal, "Password", this.authorities);
		assertThat(token.getName()).isEqualTo(principalName);
		verify(principal, times(1)).getName();
	}

	private class MockAuthenticationImpl extends AbstractAuthenticationToken {

		private Object credentials;

		private Object principal;

		MockAuthenticationImpl(Object principal, Object credentials, List<GrantedAuthority> authorities) {
			super(authorities);
			this.principal = principal;
			this.credentials = credentials;
		}

		@Override
		public Object getCredentials() {
			return this.credentials;
		}

		@Override
		public Object getPrincipal() {
			return this.principal;
		}

	}

}
