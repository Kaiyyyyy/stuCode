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

package org.springframework.security.web.authentication.rememberme;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Detects if there is no {@code Authentication} object in the {@code SecurityContext},
 * and populates the context with a remember-me authentication token if a
 * {@link RememberMeServices} implementation so requests.
 * <p>
 * Concrete {@code RememberMeServices} implementations will have their
 * {@link RememberMeServices#autoLogin(HttpServletRequest, HttpServletResponse)} method
 * called by this filter. If this method returns a non-null {@code Authentication} object,
 * it will be passed to the {@code AuthenticationManager}, so that any
 * authentication-specific behaviour can be achieved. The resulting {@code Authentication}
 * (if successful) will be placed into the {@code SecurityContext}.
 * <p>
 * If authentication is successful, an {@link InteractiveAuthenticationSuccessEvent} will
 * be published to the application context. No events will be published if authentication
 * was unsuccessful, because this would generally be recorded via an
 * {@code AuthenticationManager}-specific application event.
 * <p>
 * Normally the request will be allowed to proceed regardless of whether authentication
 * succeeds or fails. If some control over the destination for authenticated users is
 * required, an {@link AuthenticationSuccessHandler} can be injected
 *
 * @author Ben Alex
 * @author Luke Taylor
 */
public class RememberMeAuthenticationFilter extends GenericFilterBean implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher eventPublisher;

	private AuthenticationSuccessHandler successHandler;

	private AuthenticationManager authenticationManager;

	private RememberMeServices rememberMeServices;

	public RememberMeAuthenticationFilter(AuthenticationManager authenticationManager,
			RememberMeServices rememberMeServices) {
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		Assert.notNull(rememberMeServices, "rememberMeServices cannot be null");
		this.authenticationManager = authenticationManager;
		this.rememberMeServices = rememberMeServices;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.authenticationManager, "authenticationManager must be specified");
		Assert.notNull(this.rememberMeServices, "rememberMeServices must be specified");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			this.logger.debug(LogMessage
					.of(() -> "SecurityContextHolder not populated with remember-me token, as it already contained: '"
							+ SecurityContextHolder.getContext().getAuthentication() + "'"));
			chain.doFilter(request, response);
			return;
		}
		Authentication rememberMeAuth = this.rememberMeServices.autoLogin(request, response);
		if (rememberMeAuth != null) {
			// Attempt authenticaton via AuthenticationManager
			try {
				rememberMeAuth = this.authenticationManager.authenticate(rememberMeAuth);
				// Store to SecurityContextHolder
				SecurityContextHolder.getContext().setAuthentication(rememberMeAuth);
				onSuccessfulAuthentication(request, response, rememberMeAuth);
				this.logger.debug(LogMessage.of(() -> "SecurityContextHolder populated with remember-me token: '"
						+ SecurityContextHolder.getContext().getAuthentication() + "'"));
				if (this.eventPublisher != null) {
					this.eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(
							SecurityContextHolder.getContext().getAuthentication(), this.getClass()));
				}
				if (this.successHandler != null) {
					this.successHandler.onAuthenticationSuccess(request, response, rememberMeAuth);
					return;
				}
			}
			catch (AuthenticationException ex) {
				this.logger.debug(LogMessage
						.format("SecurityContextHolder not populated with remember-me token, as AuthenticationManager "
								+ "rejected Authentication returned by RememberMeServices: '%s'; "
								+ "invalidating remember-me token", rememberMeAuth),
						ex);
				this.rememberMeServices.loginFail(request, response);
				onUnsuccessfulAuthentication(request, response, ex);
			}
		}
		chain.doFilter(request, response);
	}

	/**
	 * Called if a remember-me token is presented and successfully authenticated by the
	 * {@code RememberMeServices} {@code autoLogin} method and the
	 * {@code AuthenticationManager}.
	 */
	protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			Authentication authResult) {
	}

	/**
	 * Called if the {@code AuthenticationManager} rejects the authentication object
	 * returned from the {@code RememberMeServices} {@code autoLogin} method. This method
	 * will not be called when no remember-me token is present in the request and
	 * {@code autoLogin} reurns null.
	 */
	protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) {
	}

	public RememberMeServices getRememberMeServices() {
		return this.rememberMeServices;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Allows control over the destination a remembered user is sent to when they are
	 * successfully authenticated. By default, the filter will just allow the current
	 * request to proceed, but if an {@code AuthenticationSuccessHandler} is set, it will
	 * be invoked and the {@code doFilter()} method will return immediately, thus allowing
	 * the application to redirect the user to a specific URL, regardless of whatthe
	 * original request was for.
	 * @param successHandler the strategy to invoke immediately before returning from
	 * {@code doFilter()}.
	 */
	public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler successHandler) {
		Assert.notNull(successHandler, "successHandler cannot be null");
		this.successHandler = successHandler;
	}

}
