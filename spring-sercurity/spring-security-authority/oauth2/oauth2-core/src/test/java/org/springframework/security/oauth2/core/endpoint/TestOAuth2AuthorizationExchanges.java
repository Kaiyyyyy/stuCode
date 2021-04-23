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

package org.springframework.security.oauth2.core.endpoint;

/**
 * @author Rob Winch
 * @author Eddú Meléndez
 * @since 5.1
 */
public final class TestOAuth2AuthorizationExchanges {

	private TestOAuth2AuthorizationExchanges() {
	}

	public static OAuth2AuthorizationExchange success() {
		OAuth2AuthorizationRequest request = TestOAuth2AuthorizationRequests.request().build();
		OAuth2AuthorizationResponse response = TestOAuth2AuthorizationResponses.success().build();
		return new OAuth2AuthorizationExchange(request, response);
	}

	public static OAuth2AuthorizationExchange failure() {
		OAuth2AuthorizationRequest request = TestOAuth2AuthorizationRequests.request().build();
		OAuth2AuthorizationResponse response = TestOAuth2AuthorizationResponses.error().build();
		return new OAuth2AuthorizationExchange(request, response);
	}

}
