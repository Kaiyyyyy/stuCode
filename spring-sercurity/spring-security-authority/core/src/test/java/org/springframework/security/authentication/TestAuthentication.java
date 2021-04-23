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

package org.springframework.security.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.PasswordEncodedUser;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class TestAuthentication extends PasswordEncodedUser {

	public static Authentication authenticatedAdmin() {
		return autheticated(admin());
	}

	public static Authentication authenticatedUser() {
		return autheticated(user());
	}

	public static Authentication autheticated(UserDetails user) {
		return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
	}

}
