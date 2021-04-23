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

package org.springframework.security.acls.sid;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class SidTests {

	@Test
	public void testPrincipalSidConstructorsRequiredFields() {
		// Check one String-argument constructor
		assertThatIllegalArgumentException().isThrownBy(() -> new PrincipalSid((String) null));
		assertThatIllegalArgumentException().isThrownBy(() -> new PrincipalSid(""));
		assertThatNoException().isThrownBy(() -> new PrincipalSid("johndoe"));
		// Check one Authentication-argument constructor
		assertThatIllegalArgumentException().isThrownBy(() -> new PrincipalSid((Authentication) null));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PrincipalSid(new TestingAuthenticationToken(null, "password")));
		assertThatNoException()
				.isThrownBy(() -> new PrincipalSid(new TestingAuthenticationToken("johndoe", "password")));
	}

	@Test
	public void testGrantedAuthoritySidConstructorsRequiredFields() {
		// Check one String-argument constructor
		assertThatIllegalArgumentException().isThrownBy(() -> new GrantedAuthoritySid((String) null));
		assertThatIllegalArgumentException().isThrownBy(() -> new GrantedAuthoritySid(""));
		assertThatNoException().isThrownBy(() -> new GrantedAuthoritySid("ROLE_TEST"));
		// Check one GrantedAuthority-argument constructor
		assertThatIllegalArgumentException().isThrownBy(() -> new GrantedAuthoritySid((GrantedAuthority) null));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new GrantedAuthoritySid(new SimpleGrantedAuthority(null)));
		assertThatNoException().isThrownBy(() -> new GrantedAuthoritySid(new SimpleGrantedAuthority("ROLE_TEST")));
	}

	@Test
	public void testPrincipalSidEquals() {
		Authentication authentication = new TestingAuthenticationToken("johndoe", "password");
		Sid principalSid = new PrincipalSid(authentication);
		assertThat(principalSid.equals(null)).isFalse();
		assertThat(principalSid.equals("DIFFERENT_TYPE_OBJECT")).isFalse();
		assertThat(principalSid.equals(principalSid)).isTrue();
		assertThat(principalSid.equals(new PrincipalSid(authentication))).isTrue();
		assertThat(principalSid.equals(new PrincipalSid(new TestingAuthenticationToken("johndoe", null)))).isTrue();
		assertThat(principalSid.equals(new PrincipalSid(new TestingAuthenticationToken("scott", null)))).isFalse();
		assertThat(principalSid.equals(new PrincipalSid("johndoe"))).isTrue();
		assertThat(principalSid.equals(new PrincipalSid("scott"))).isFalse();
	}

	@Test
	public void testGrantedAuthoritySidEquals() {
		GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_TEST");
		Sid gaSid = new GrantedAuthoritySid(ga);
		assertThat(gaSid.equals(null)).isFalse();
		assertThat(gaSid.equals("DIFFERENT_TYPE_OBJECT")).isFalse();
		assertThat(gaSid.equals(gaSid)).isTrue();
		assertThat(gaSid.equals(new GrantedAuthoritySid(ga))).isTrue();
		assertThat(gaSid.equals(new GrantedAuthoritySid(new SimpleGrantedAuthority("ROLE_TEST")))).isTrue();
		assertThat(gaSid.equals(new GrantedAuthoritySid(new SimpleGrantedAuthority("ROLE_NOT_EQUAL")))).isFalse();
		assertThat(gaSid.equals(new GrantedAuthoritySid("ROLE_TEST"))).isTrue();
		assertThat(gaSid.equals(new GrantedAuthoritySid("ROLE_NOT_EQUAL"))).isFalse();
	}

	@Test
	public void testPrincipalSidHashCode() {
		Authentication authentication = new TestingAuthenticationToken("johndoe", "password");
		Sid principalSid = new PrincipalSid(authentication);
		assertThat(principalSid.hashCode()).isEqualTo("johndoe".hashCode());
		assertThat(principalSid.hashCode()).isEqualTo(new PrincipalSid("johndoe").hashCode());
		assertThat(principalSid.hashCode()).isNotEqualTo(new PrincipalSid("scott").hashCode());
		assertThat(principalSid.hashCode())
				.isNotEqualTo(new PrincipalSid(new TestingAuthenticationToken("scott", "password")).hashCode());
	}

	@Test
	public void testGrantedAuthoritySidHashCode() {
		GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_TEST");
		Sid gaSid = new GrantedAuthoritySid(ga);
		assertThat(gaSid.hashCode()).isEqualTo("ROLE_TEST".hashCode());
		assertThat(gaSid.hashCode()).isEqualTo(new GrantedAuthoritySid("ROLE_TEST").hashCode());
		assertThat(gaSid.hashCode()).isNotEqualTo(new GrantedAuthoritySid("ROLE_TEST_2").hashCode());
		assertThat(gaSid.hashCode())
				.isNotEqualTo(new GrantedAuthoritySid(new SimpleGrantedAuthority("ROLE_TEST_2")).hashCode());
	}

	@Test
	public void testGetters() {
		Authentication authentication = new TestingAuthenticationToken("johndoe", "password");
		PrincipalSid principalSid = new PrincipalSid(authentication);
		GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_TEST");
		GrantedAuthoritySid gaSid = new GrantedAuthoritySid(ga);
		assertThat("johndoe".equals(principalSid.getPrincipal())).isTrue();
		assertThat("scott".equals(principalSid.getPrincipal())).isFalse();
		assertThat("ROLE_TEST".equals(gaSid.getGrantedAuthority())).isTrue();
		assertThat("ROLE_TEST2".equals(gaSid.getGrantedAuthority())).isFalse();
	}

	@Test
	public void getPrincipalWhenPrincipalInstanceOfUserDetailsThenReturnsUsername() {
		User user = new User("user", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEST")));
		Authentication authentication = new TestingAuthenticationToken(user, "password");
		PrincipalSid principalSid = new PrincipalSid(authentication);
		assertThat("user").isEqualTo(principalSid.getPrincipal());
	}

	@Test
	public void getPrincipalWhenPrincipalNotInstanceOfUserDetailsThenReturnsPrincipalName() {
		Authentication authentication = new TestingAuthenticationToken("token", "password");
		PrincipalSid principalSid = new PrincipalSid(authentication);
		assertThat("token").isEqualTo(principalSid.getPrincipal());
	}

	@Test
	public void getPrincipalWhenCustomAuthenticationPrincipalThenReturnsPrincipalName() {
		Authentication authentication = new CustomAuthenticationToken(new CustomToken("token"), null);
		PrincipalSid principalSid = new PrincipalSid(authentication);
		assertThat("token").isEqualTo(principalSid.getPrincipal());
	}

	static class CustomAuthenticationToken extends AbstractAuthenticationToken {

		private CustomToken principal;

		CustomAuthenticationToken(CustomToken principal, Collection<GrantedAuthority> authorities) {
			super(authorities);
			this.principal = principal;
		}

		@Override
		public Object getCredentials() {
			return null;
		}

		@Override
		public CustomToken getPrincipal() {
			return this.principal;
		}

		@Override
		public String getName() {
			return this.principal.getName();
		}

	}

	static class CustomToken {

		private String name;

		CustomToken(String name) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

}
