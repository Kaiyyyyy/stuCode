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

package org.springframework.security.oauth2.client.userinfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Represents a request the {@link OAuth2UserService} uses when initiating a request to
 * the UserInfo Endpoint.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see ClientRegistration
 * @see OAuth2AccessToken
 * @see OAuth2UserService
 */
public class OAuth2UserRequest {

	private final ClientRegistration clientRegistration;

	private final OAuth2AccessToken accessToken;

	private final Map<String, Object> additionalParameters;

	/**
	 * Constructs an {@code OAuth2UserRequest} using the provided parameters.
	 * @param clientRegistration the client registration
	 * @param accessToken the access token
	 */
	public OAuth2UserRequest(ClientRegistration clientRegistration, OAuth2AccessToken accessToken) {
		this(clientRegistration, accessToken, Collections.emptyMap());
	}

	/**
	 * Constructs an {@code OAuth2UserRequest} using the provided parameters.
	 * @param clientRegistration the client registration
	 * @param accessToken the access token
	 * @param additionalParameters the additional parameters, may be empty
	 * @since 5.1
	 */
	public OAuth2UserRequest(ClientRegistration clientRegistration, OAuth2AccessToken accessToken,
			Map<String, Object> additionalParameters) {
		Assert.notNull(clientRegistration, "clientRegistration cannot be null");
		Assert.notNull(accessToken, "accessToken cannot be null");
		this.clientRegistration = clientRegistration;
		this.accessToken = accessToken;
		this.additionalParameters = Collections.unmodifiableMap(CollectionUtils.isEmpty(additionalParameters)
				? Collections.emptyMap() : new LinkedHashMap<>(additionalParameters));
	}

	/**
	 * Returns the {@link ClientRegistration client registration}.
	 * @return the {@link ClientRegistration}
	 */
	public ClientRegistration getClientRegistration() {
		return this.clientRegistration;
	}

	/**
	 * Returns the {@link OAuth2AccessToken access token}.
	 * @return the {@link OAuth2AccessToken}
	 */
	public OAuth2AccessToken getAccessToken() {
		return this.accessToken;
	}

	/**
	 * Returns the additional parameters that may be used in the request.
	 * @return a {@code Map} of the additional parameters, may be empty.
	 * @since 5.1
	 */
	public Map<String, Object> getAdditionalParameters() {
		return this.additionalParameters;
	}

}
