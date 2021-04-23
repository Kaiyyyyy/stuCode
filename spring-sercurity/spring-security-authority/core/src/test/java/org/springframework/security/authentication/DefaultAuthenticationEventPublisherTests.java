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

package org.springframework.security.authentication;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureCredentialsExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationFailureProviderNotFoundEvent;
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Luke Taylor
 */
public class DefaultAuthenticationEventPublisherTests {

	DefaultAuthenticationEventPublisher publisher;

	@Test
	public void expectedDefaultMappingsAreSatisfied() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
		this.publisher.setApplicationEventPublisher(appPublisher);
		Authentication a = mock(Authentication.class);
		Exception cause = new Exception();
		Object extraInfo = new Object();
		this.publisher.publishAuthenticationFailure(new BadCredentialsException(""), a);
		this.publisher.publishAuthenticationFailure(new BadCredentialsException("", cause), a);
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureBadCredentialsEvent.class));
		reset(appPublisher);
		this.publisher.publishAuthenticationFailure(new UsernameNotFoundException(""), a);
		this.publisher.publishAuthenticationFailure(new UsernameNotFoundException("", cause), a);
		this.publisher.publishAuthenticationFailure(new AccountExpiredException(""), a);
		this.publisher.publishAuthenticationFailure(new AccountExpiredException("", cause), a);
		this.publisher.publishAuthenticationFailure(new ProviderNotFoundException(""), a);
		this.publisher.publishAuthenticationFailure(new DisabledException(""), a);
		this.publisher.publishAuthenticationFailure(new DisabledException("", cause), a);
		this.publisher.publishAuthenticationFailure(new LockedException(""), a);
		this.publisher.publishAuthenticationFailure(new LockedException("", cause), a);
		this.publisher.publishAuthenticationFailure(new AuthenticationServiceException(""), a);
		this.publisher.publishAuthenticationFailure(new AuthenticationServiceException("", cause), a);
		this.publisher.publishAuthenticationFailure(new CredentialsExpiredException(""), a);
		this.publisher.publishAuthenticationFailure(new CredentialsExpiredException("", cause), a);
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureBadCredentialsEvent.class));
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureExpiredEvent.class));
		verify(appPublisher).publishEvent(isA(AuthenticationFailureProviderNotFoundEvent.class));
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureDisabledEvent.class));
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureLockedEvent.class));
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureServiceExceptionEvent.class));
		verify(appPublisher, times(2)).publishEvent(isA(AuthenticationFailureCredentialsExpiredEvent.class));
		verifyNoMoreInteractions(appPublisher);
	}

	@Test
	public void authenticationSuccessIsPublished() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
		this.publisher.setApplicationEventPublisher(appPublisher);
		this.publisher.publishAuthenticationSuccess(mock(Authentication.class));
		verify(appPublisher).publishEvent(isA(AuthenticationSuccessEvent.class));
		this.publisher.setApplicationEventPublisher(null);
		// Should be ignored with null app publisher
		this.publisher.publishAuthenticationSuccess(mock(Authentication.class));
	}

	@Test
	public void additionalExceptionMappingsAreSupported() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		Properties p = new Properties();
		p.put(MockAuthenticationException.class.getName(), AuthenticationFailureDisabledEvent.class.getName());
		this.publisher.setAdditionalExceptionMappings(p);
		ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
		this.publisher.setApplicationEventPublisher(appPublisher);
		this.publisher.publishAuthenticationFailure(new MockAuthenticationException("test"),
				mock(Authentication.class));
		verify(appPublisher).publishEvent(isA(AuthenticationFailureDisabledEvent.class));
	}

	@Test
	public void missingEventClassExceptionCausesException() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		Properties p = new Properties();
		p.put(MockAuthenticationException.class.getName(), "NoSuchClass");
		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> this.publisher.setAdditionalExceptionMappings(p));
	}

	@Test
	public void unknownFailureExceptionIsIgnored() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		Properties p = new Properties();
		p.put(MockAuthenticationException.class.getName(), AuthenticationFailureDisabledEvent.class.getName());
		this.publisher.setAdditionalExceptionMappings(p);
		ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
		this.publisher.setApplicationEventPublisher(appPublisher);
		this.publisher.publishAuthenticationFailure(new AuthenticationException("") {
		}, mock(Authentication.class));
		verifyZeroInteractions(appPublisher);
	}

	@Test
	public void emptyMapCausesException() {
		Map<Class<? extends AuthenticationException>, Class<? extends AbstractAuthenticationFailureEvent>> mappings = new HashMap<>();
		this.publisher = new DefaultAuthenticationEventPublisher();
		assertThatIllegalArgumentException().isThrownBy(() -> this.publisher.setAdditionalExceptionMappings(mappings));
	}

	@Test
	public void missingExceptionClassCausesException() {
		Map<Class<? extends AuthenticationException>, Class<? extends AbstractAuthenticationFailureEvent>> mappings = new HashMap<>();
		mappings.put(null, AuthenticationFailureLockedEvent.class);
		this.publisher = new DefaultAuthenticationEventPublisher();
		assertThatIllegalArgumentException().isThrownBy(() -> this.publisher.setAdditionalExceptionMappings(mappings));
	}

	@Test
	public void missingEventClassAsMapValueCausesException() {
		Map<Class<? extends AuthenticationException>, Class<? extends AbstractAuthenticationFailureEvent>> mappings = new HashMap<>();
		mappings.put(LockedException.class, null);
		this.publisher = new DefaultAuthenticationEventPublisher();
		assertThatIllegalArgumentException().isThrownBy(() -> this.publisher.setAdditionalExceptionMappings(mappings));
	}

	@Test
	public void additionalExceptionMappingsUsingMapAreSupported() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		Map<Class<? extends AuthenticationException>, Class<? extends AbstractAuthenticationFailureEvent>> mappings = new HashMap<>();
		mappings.put(MockAuthenticationException.class, AuthenticationFailureDisabledEvent.class);
		this.publisher.setAdditionalExceptionMappings(mappings);
		ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
		this.publisher.setApplicationEventPublisher(appPublisher);
		this.publisher.publishAuthenticationFailure(new MockAuthenticationException("test"),
				mock(Authentication.class));
		verify(appPublisher).publishEvent(isA(AuthenticationFailureDisabledEvent.class));
	}

	@Test
	public void defaultAuthenticationFailureEventClassSetNullThen() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.publisher.setDefaultAuthenticationFailureEvent(null));
	}

	@Test
	public void defaultAuthenticationFailureEventIsPublished() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		this.publisher.setDefaultAuthenticationFailureEvent(AuthenticationFailureBadCredentialsEvent.class);
		ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
		this.publisher.setApplicationEventPublisher(appPublisher);
		this.publisher.publishAuthenticationFailure(new AuthenticationException("") {
		}, mock(Authentication.class));
		verify(appPublisher).publishEvent(isA(AuthenticationFailureBadCredentialsEvent.class));
	}

	@Test
	public void defaultAuthenticationFailureEventMissingAppropriateConstructorThen() {
		this.publisher = new DefaultAuthenticationEventPublisher();
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.publisher
				.setDefaultAuthenticationFailureEvent(AuthenticationFailureEventWithoutAppropriateConstructor.class));
	}

	private static final class AuthenticationFailureEventWithoutAppropriateConstructor
			extends AbstractAuthenticationFailureEvent {

		AuthenticationFailureEventWithoutAppropriateConstructor(Authentication auth) {
			super(auth, new AuthenticationException("") {
			});
		}

	}

	private static final class MockAuthenticationException extends AuthenticationException {

		MockAuthenticationException(String msg) {
			super(msg);
		}

	}

}
