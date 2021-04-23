/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.security.web.csrf;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>
 * Applies
 * <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)" >CSRF</a>
 * protection using a synchronizer token pattern. Developers are required to ensure that
 * {@link CsrfFilter} is invoked for any request that allows state to change. Typically
 * this just means that they should ensure their web application follows proper REST
 * semantics (i.e. do not change state with the HTTP methods GET, HEAD, TRACE, OPTIONS).
 * </p>
 *
 * <p>
 * Typically the {@link CsrfTokenRepository} implementation chooses to store the
 * {@link CsrfToken} in {@link HttpSession} with {@link HttpSessionCsrfTokenRepository}
 * wrapped by a {@link LazyCsrfTokenRepository}. This is preferred to storing the token in
 * a cookie which can be modified by a client application.
 * </p>
 *
 * @author Rob Winch
 * @since 3.2
 */
public final class CsrfFilter extends OncePerRequestFilter {

	/**
	 * The default {@link RequestMatcher} that indicates if CSRF protection is required or
	 * not. The default is to ignore GET, HEAD, TRACE, OPTIONS and process all other
	 * requests.
	 */
	public static final RequestMatcher DEFAULT_CSRF_MATCHER = new DefaultRequiresCsrfMatcher();

	/**
	 * The attribute name to use when marking a given request as one that should not be
	 * filtered.
	 *
	 * To use, set the attribute on your {@link HttpServletRequest}: <pre>
	 * 	CsrfFilter.skipRequest(request);
	 * </pre>
	 */
	private static final String SHOULD_NOT_FILTER = "SHOULD_NOT_FILTER" + CsrfFilter.class.getName();

	private final Log logger = LogFactory.getLog(getClass());

	private final CsrfTokenRepository tokenRepository;

	private RequestMatcher requireCsrfProtectionMatcher = DEFAULT_CSRF_MATCHER;

	private AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();

	public CsrfFilter(CsrfTokenRepository csrfTokenRepository) {
		Assert.notNull(csrfTokenRepository, "csrfTokenRepository cannot be null");
		this.tokenRepository = csrfTokenRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return Boolean.TRUE.equals(request.getAttribute(SHOULD_NOT_FILTER));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		request.setAttribute(HttpServletResponse.class.getName(), response);
		CsrfToken csrfToken = this.tokenRepository.loadToken(request);
		boolean missingToken = (csrfToken == null);
		if (missingToken) {
			csrfToken = this.tokenRepository.generateToken(request);
			this.tokenRepository.saveToken(csrfToken, request, response);
		}
		request.setAttribute(CsrfToken.class.getName(), csrfToken);
		request.setAttribute(csrfToken.getParameterName(), csrfToken);
		if (!this.requireCsrfProtectionMatcher.matches(request)) {
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("Did not protect against CSRF since request did not match "
						+ this.requireCsrfProtectionMatcher);
			}
			filterChain.doFilter(request, response);
			return;
		}
		String actualToken = request.getHeader(csrfToken.getHeaderName());
		if (actualToken == null) {
			actualToken = request.getParameter(csrfToken.getParameterName());
		}
		if (!equalsConstantTime(csrfToken.getToken(), actualToken)) {
			this.logger.debug(
					LogMessage.of(() -> "Invalid CSRF token found for " + UrlUtils.buildFullRequestUrl(request)));
			AccessDeniedException exception = (!missingToken) ? new InvalidCsrfTokenException(csrfToken, actualToken)
					: new MissingCsrfTokenException(actualToken);
			this.accessDeniedHandler.handle(request, response, exception);
			return;
		}
		filterChain.doFilter(request, response);
	}

	public static void skipRequest(HttpServletRequest request) {
		request.setAttribute(SHOULD_NOT_FILTER, Boolean.TRUE);
	}

	/**
	 * Specifies a {@link RequestMatcher} that is used to determine if CSRF protection
	 * should be applied. If the {@link RequestMatcher} returns true for a given request,
	 * then CSRF protection is applied.
	 *
	 * <p>
	 * The default is to apply CSRF protection for any HTTP method other than GET, HEAD,
	 * TRACE, OPTIONS.
	 * </p>
	 * @param requireCsrfProtectionMatcher the {@link RequestMatcher} used to determine if
	 * CSRF protection should be applied.
	 */
	public void setRequireCsrfProtectionMatcher(RequestMatcher requireCsrfProtectionMatcher) {
		Assert.notNull(requireCsrfProtectionMatcher, "requireCsrfProtectionMatcher cannot be null");
		this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
	}

	/**
	 * Specifies a {@link AccessDeniedHandler} that should be used when CSRF protection
	 * fails.
	 *
	 * <p>
	 * The default is to use AccessDeniedHandlerImpl with no arguments.
	 * </p>
	 * @param accessDeniedHandler the {@link AccessDeniedHandler} to use
	 */
	public void setAccessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
		Assert.notNull(accessDeniedHandler, "accessDeniedHandler cannot be null");
		this.accessDeniedHandler = accessDeniedHandler;
	}

	/**
	 * Constant time comparison to prevent against timing attacks.
	 * @param expected
	 * @param actual
	 * @return
	 */
	private static boolean equalsConstantTime(String expected, String actual) {
		byte[] expectedBytes = bytesUtf8(expected);
		byte[] actualBytes = bytesUtf8(actual);
		return MessageDigest.isEqual(expectedBytes, actualBytes);
	}

	private static byte[] bytesUtf8(String s) {
		// need to check if Utf8.encode() runs in constant time (probably not).
		// This may leak length of string.
		return (s != null) ? Utf8.encode(s) : null;
	}

	private static final class DefaultRequiresCsrfMatcher implements RequestMatcher {

		private final HashSet<String> allowedMethods = new HashSet<>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));

		@Override
		public boolean matches(HttpServletRequest request) {
			return !this.allowedMethods.contains(request.getMethod());
		}

		@Override
		public String toString() {
			return "CsrfNotRequired " + this.allowedMethods;
		}

	}

}
