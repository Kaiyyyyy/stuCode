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

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.TestOidcUsers;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.TestOAuth2Users;

/**
 * @author Josh Cummings
 * @since 5.2
 */
public final class TestOAuth2AuthenticationTokens {

	private TestOAuth2AuthenticationTokens() {
	}

	public static OAuth2AuthenticationToken authenticated() {
		DefaultOAuth2User principal = TestOAuth2Users.create();
		String registrationId = "registration-id";
		return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId);
	}

	public static OAuth2AuthenticationToken oidcAuthenticated() {
		DefaultOidcUser principal = TestOidcUsers.create();
		String registrationId = "registration-id";
		return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId);
	}

}
