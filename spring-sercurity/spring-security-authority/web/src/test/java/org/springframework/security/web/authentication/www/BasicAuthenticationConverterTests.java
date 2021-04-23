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

package org.springframework.security.web.authentication.www;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Sergey Bespalov
 * @since 5.2.0
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicAuthenticationConverterTests {

	@Mock
	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

	private BasicAuthenticationConverter converter;

	@Before
	public void setup() {
		this.converter = new BasicAuthenticationConverter(this.authenticationDetailsSource);
	}

	@Test
	public void testNormalOperation() {
		String token = "rod:koala";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64(token.getBytes())));
		UsernamePasswordAuthenticationToken authentication = this.converter.convert(request);
		verify(this.authenticationDetailsSource).buildDetails(any());
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("rod");
	}

	@Test
	public void requestWhenAuthorizationSchemeInMixedCaseThenAuthenticates() {
		String token = "rod:koala";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "BaSiC " + new String(Base64.encodeBase64(token.getBytes())));
		UsernamePasswordAuthenticationToken authentication = this.converter.convert(request);
		verify(this.authenticationDetailsSource).buildDetails(any());
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("rod");
	}

	@Test
	public void testWhenUnsupportedAuthorizationHeaderThenIgnored() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer someOtherToken");
		UsernamePasswordAuthenticationToken authentication = this.converter.convert(request);
		verifyZeroInteractions(this.authenticationDetailsSource);
		assertThat(authentication).isNull();
	}

	@Test
	public void testWhenInvalidBasicAuthorizationTokenThenError() {
		String token = "NOT_A_VALID_TOKEN_AS_MISSING_COLON";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64(token.getBytes())));
		assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(() -> this.converter.convert(request));
	}

	@Test
	public void testWhenInvalidBase64ThenError() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic NOT_VALID_BASE64");
		assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(() -> this.converter.convert(request));
	}

	@Test
	public void convertWhenEmptyPassword() {
		String token = "rod:";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64(token.getBytes())));
		UsernamePasswordAuthenticationToken authentication = this.converter.convert(request);
		verify(this.authenticationDetailsSource).buildDetails(any());
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("rod");
		assertThat(authentication.getCredentials()).isEqualTo("");
	}

	@Test
	public void requestWhenEmptyBasicAuthorizationHeaderTokenThenError() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic ");
		assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(() -> this.converter.convert(request));
	}

}
