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

package org.springframework.security.oauth2.core.http.converter;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OAuth2AccessTokenResponseHttpMessageConverter}.
 *
 * @author Joe Grandja
 */
public class OAuth2AccessTokenResponseHttpMessageConverterTests {

	private OAuth2AccessTokenResponseHttpMessageConverter messageConverter;

	@Before
	public void setup() {
		this.messageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
	}

	@Test
	public void supportsWhenOAuth2AccessTokenResponseThenTrue() {
		assertThat(this.messageConverter.supports(OAuth2AccessTokenResponse.class)).isTrue();
	}

	@Test
	public void setTokenResponseConverterWhenConverterIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.messageConverter.setTokenResponseConverter(null));
	}

	@Test
	public void setTokenResponseParametersConverterWhenConverterIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.messageConverter.setTokenResponseParametersConverter(null));
	}

	@Test
	public void readInternalWhenSuccessfulTokenResponseThenReadOAuth2AccessTokenResponse() throws Exception {
		// @formatter:off
		String tokenResponse = "{\n"
			+ "   \"access_token\": \"access-token-1234\",\n"
			+ "   \"token_type\": \"bearer\",\n"
			+ "   \"expires_in\": \"3600\",\n"
			+ "   \"scope\": \"read write\",\n"
			+ "   \"refresh_token\": \"refresh-token-1234\",\n"
			+ "   \"custom_parameter_1\": \"custom-value-1\",\n"
			+ "   \"custom_parameter_2\": \"custom-value-2\"\n"
			+ "}\n";
		// @formatter:on
		MockClientHttpResponse response = new MockClientHttpResponse(tokenResponse.getBytes(), HttpStatus.OK);
		OAuth2AccessTokenResponse accessTokenResponse = this.messageConverter
				.readInternal(OAuth2AccessTokenResponse.class, response);
		assertThat(accessTokenResponse.getAccessToken().getTokenValue()).isEqualTo("access-token-1234");
		assertThat(accessTokenResponse.getAccessToken().getTokenType()).isEqualTo(OAuth2AccessToken.TokenType.BEARER);
		assertThat(accessTokenResponse.getAccessToken().getExpiresAt())
				.isBeforeOrEqualTo(Instant.now().plusSeconds(3600));
		assertThat(accessTokenResponse.getAccessToken().getScopes()).containsExactly("read", "write");
		assertThat(accessTokenResponse.getRefreshToken().getTokenValue()).isEqualTo("refresh-token-1234");
		assertThat(accessTokenResponse.getAdditionalParameters()).containsExactly(
				entry("custom_parameter_1", "custom-value-1"), entry("custom_parameter_2", "custom-value-2"));
	}

	// gh-6463
	@Test
	public void readInternalWhenSuccessfulTokenResponseWithObjectThenReadOAuth2AccessTokenResponse() {
		// @formatter:off
		String tokenResponse = "{\n"
			+ "   \"access_token\": \"access-token-1234\",\n"
			+ "   \"token_type\": \"bearer\",\n"
			+ "   \"expires_in\": 3600,\n"
			+ "   \"scope\": \"read write\",\n"
			+ "   \"refresh_token\": \"refresh-token-1234\",\n"
			+ "   \"custom_object_1\": {\"name1\": \"value1\"},\n"
			+ "   \"custom_object_2\": [\"value1\", \"value2\"],\n"
			+ "   \"custom_parameter_1\": \"custom-value-1\",\n"
			+ "   \"custom_parameter_2\": \"custom-value-2\"\n"
			+ "}\n";
		// @formatter:on
		MockClientHttpResponse response = new MockClientHttpResponse(tokenResponse.getBytes(), HttpStatus.OK);
		OAuth2AccessTokenResponse accessTokenResponse = this.messageConverter
				.readInternal(OAuth2AccessTokenResponse.class, response);
		assertThat(accessTokenResponse.getAccessToken().getTokenValue()).isEqualTo("access-token-1234");
		assertThat(accessTokenResponse.getAccessToken().getTokenType()).isEqualTo(OAuth2AccessToken.TokenType.BEARER);
		assertThat(accessTokenResponse.getAccessToken().getExpiresAt())
				.isBeforeOrEqualTo(Instant.now().plusSeconds(3600));
		assertThat(accessTokenResponse.getAccessToken().getScopes()).containsExactly("read", "write");
		assertThat(accessTokenResponse.getRefreshToken().getTokenValue()).isEqualTo("refresh-token-1234");
		assertThat(accessTokenResponse.getAdditionalParameters()).containsExactly(
				entry("custom_object_1", "{name1=value1}"), entry("custom_object_2", "[value1, value2]"),
				entry("custom_parameter_1", "custom-value-1"), entry("custom_parameter_2", "custom-value-2"));
	}

	// gh-8108
	@Test
	public void readInternalWhenSuccessfulTokenResponseWithNullValueThenReadOAuth2AccessTokenResponse() {
		// @formatter:off
		String tokenResponse = "{\n"
			+ "   \"access_token\": \"access-token-1234\",\n"
			+ "   \"token_type\": \"bearer\",\n"
			+ "   \"expires_in\": 3600,\n"
			+ "   \"scope\": null,\n"
			+ "   \"refresh_token\": \"refresh-token-1234\"\n"
			+ "}\n";
		// @formatter:on
		MockClientHttpResponse response = new MockClientHttpResponse(tokenResponse.getBytes(), HttpStatus.OK);
		OAuth2AccessTokenResponse accessTokenResponse = this.messageConverter
				.readInternal(OAuth2AccessTokenResponse.class, response);
		assertThat(accessTokenResponse.getAccessToken().getTokenValue()).isEqualTo("access-token-1234");
		assertThat(accessTokenResponse.getAccessToken().getTokenType()).isEqualTo(OAuth2AccessToken.TokenType.BEARER);
		assertThat(accessTokenResponse.getAccessToken().getExpiresAt())
				.isBeforeOrEqualTo(Instant.now().plusSeconds(3600));
		assertThat(accessTokenResponse.getAccessToken().getScopes()).containsExactly("null");
		assertThat(accessTokenResponse.getRefreshToken().getTokenValue()).isEqualTo("refresh-token-1234");
	}

	@Test
	public void readInternalWhenConversionFailsThenThrowHttpMessageNotReadableException() {
		Converter tokenResponseConverter = mock(Converter.class);
		given(tokenResponseConverter.convert(any())).willThrow(RuntimeException.class);
		this.messageConverter.setTokenResponseConverter(tokenResponseConverter);
		String tokenResponse = "{}";
		MockClientHttpResponse response = new MockClientHttpResponse(tokenResponse.getBytes(), HttpStatus.OK);
		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> this.messageConverter.readInternal(OAuth2AccessTokenResponse.class, response))
				.withMessageContaining("An error occurred reading the OAuth 2.0 Access Token Response");
	}

	@Test
	public void writeInternalWhenOAuth2AccessTokenResponseThenWriteTokenResponse() throws Exception {
		Instant expiresAt = Instant.now().plusSeconds(3600);
		Set<String> scopes = new LinkedHashSet<>(Arrays.asList("read", "write"));
		Map<String, Object> additionalParameters = new HashMap<>();
		additionalParameters.put("custom_parameter_1", "custom-value-1");
		additionalParameters.put("custom_parameter_2", "custom-value-2");
		// @formatter:off
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse.withToken("access-token-1234")
				.tokenType(OAuth2AccessToken.TokenType.BEARER)
				.expiresIn(expiresAt.toEpochMilli())
				.scopes(scopes)
				.refreshToken("refresh-token-1234")
				.additionalParameters(additionalParameters)
				.build();
		// @formatter:on
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.messageConverter.writeInternal(accessTokenResponse, outputMessage);
		String tokenResponse = outputMessage.getBodyAsString();
		assertThat(tokenResponse).contains("\"access_token\":\"access-token-1234\"");
		assertThat(tokenResponse).contains("\"token_type\":\"Bearer\"");
		assertThat(tokenResponse).contains("\"expires_in\"");
		assertThat(tokenResponse).contains("\"scope\":\"read write\"");
		assertThat(tokenResponse).contains("\"refresh_token\":\"refresh-token-1234\"");
		assertThat(tokenResponse).contains("\"custom_parameter_1\":\"custom-value-1\"");
		assertThat(tokenResponse).contains("\"custom_parameter_2\":\"custom-value-2\"");
	}

	@Test
	public void writeInternalWhenConversionFailsThenThrowHttpMessageNotWritableException() {
		Converter tokenResponseParametersConverter = mock(Converter.class);
		given(tokenResponseParametersConverter.convert(any())).willThrow(RuntimeException.class);
		this.messageConverter.setTokenResponseParametersConverter(tokenResponseParametersConverter);
		// @formatter:off
		OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse
				.withToken("access-token-1234")
				.tokenType(OAuth2AccessToken.TokenType.BEARER)
				.expiresIn(Instant.now().plusSeconds(3600)
				.toEpochMilli())
				.build();
		// @formatter:on
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		assertThatExceptionOfType(HttpMessageNotWritableException.class)
				.isThrownBy(() -> this.messageConverter.writeInternal(accessTokenResponse, outputMessage))
				.withMessageContaining("An error occurred writing the OAuth 2.0 Access Token Response");
	}

}
