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

package org.springframework.security.web.server.csrf;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * <p>
 * Applies
 * <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)" >CSRF</a>
 * protection using a synchronizer token pattern. Developers are required to ensure that
 * {@link CsrfWebFilter} is invoked for any request that allows state to change. Typically
 * this just means that they should ensure their web application follows proper REST
 * semantics (i.e. do not change state with the HTTP methods GET, HEAD, TRACE, OPTIONS).
 * </p>
 *
 * <p>
 * Typically the {@link ServerCsrfTokenRepository} implementation chooses to store the
 * {@link CsrfToken} in {@link org.springframework.web.server.WebSession} with
 * {@link WebSessionServerCsrfTokenRepository}. This is preferred to storing the token in
 * a cookie which can be modified by a client application.
 * </p>
 * <p>
 * The {@code Mono&lt;CsrfToken&gt;} is exposes as a request attribute with the name of
 * {@code CsrfToken.class.getName()}. If the token is new it will automatically be saved
 * at the time it is subscribed.
 * </p>
 *
 * @author Rob Winch
 * @author Parikshit Dutta
 * @since 5.0
 */
public class CsrfWebFilter implements WebFilter {

	public static final ServerWebExchangeMatcher DEFAULT_CSRF_MATCHER = new DefaultRequireCsrfProtectionMatcher();

	/**
	 * The attribute name to use when marking a given request as one that should not be
	 * filtered.
	 *
	 * To use, set the attribute on your {@link ServerWebExchange}: <pre>
	 * 	CsrfWebFilter.skipExchange(exchange);
	 * </pre>
	 */
	private static final String SHOULD_NOT_FILTER = "SHOULD_NOT_FILTER" + CsrfWebFilter.class.getName();

	private ServerWebExchangeMatcher requireCsrfProtectionMatcher = DEFAULT_CSRF_MATCHER;

	private ServerCsrfTokenRepository csrfTokenRepository = new WebSessionServerCsrfTokenRepository();

	private ServerAccessDeniedHandler accessDeniedHandler = new HttpStatusServerAccessDeniedHandler(
			HttpStatus.FORBIDDEN);

	private boolean isTokenFromMultipartDataEnabled;

	public void setAccessDeniedHandler(ServerAccessDeniedHandler accessDeniedHandler) {
		Assert.notNull(accessDeniedHandler, "accessDeniedHandler");
		this.accessDeniedHandler = accessDeniedHandler;
	}

	public void setCsrfTokenRepository(ServerCsrfTokenRepository csrfTokenRepository) {
		Assert.notNull(csrfTokenRepository, "csrfTokenRepository cannot be null");
		this.csrfTokenRepository = csrfTokenRepository;
	}

	public void setRequireCsrfProtectionMatcher(ServerWebExchangeMatcher requireCsrfProtectionMatcher) {
		Assert.notNull(requireCsrfProtectionMatcher, "requireCsrfProtectionMatcher cannot be null");
		this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
	}

	/**
	 * Specifies if the {@code CsrfWebFilter} should try to resolve the actual CSRF token
	 * from the body of multipart data requests.
	 * @param tokenFromMultipartDataEnabled true if should read from multipart form body,
	 * else false. Default is false
	 */
	public void setTokenFromMultipartDataEnabled(boolean tokenFromMultipartDataEnabled) {
		this.isTokenFromMultipartDataEnabled = tokenFromMultipartDataEnabled;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (Boolean.TRUE.equals(exchange.getAttribute(SHOULD_NOT_FILTER))) {
			return chain.filter(exchange).then(Mono.empty());
		}
		return this.requireCsrfProtectionMatcher.matches(exchange).filter(MatchResult::isMatch)
				.filter((matchResult) -> !exchange.getAttributes().containsKey(CsrfToken.class.getName()))
				.flatMap((m) -> validateToken(exchange)).flatMap((m) -> continueFilterChain(exchange, chain))
				.switchIfEmpty(continueFilterChain(exchange, chain).then(Mono.empty()))
				.onErrorResume(CsrfException.class, (ex) -> this.accessDeniedHandler.handle(exchange, ex));
	}

	public static void skipExchange(ServerWebExchange exchange) {
		exchange.getAttributes().put(SHOULD_NOT_FILTER, Boolean.TRUE);
	}

	private Mono<Void> validateToken(ServerWebExchange exchange) {
		return this.csrfTokenRepository.loadToken(exchange)
				.switchIfEmpty(Mono
						.defer(() -> Mono.error(new CsrfException("CSRF Token has been associated to this client"))))
				.filterWhen((expected) -> containsValidCsrfToken(exchange, expected))
				.switchIfEmpty(Mono.defer(() -> Mono.error(new CsrfException("Invalid CSRF Token")))).then();
	}

	private Mono<Boolean> containsValidCsrfToken(ServerWebExchange exchange, CsrfToken expected) {
		return exchange.getFormData().flatMap((data) -> Mono.justOrEmpty(data.getFirst(expected.getParameterName())))
				.switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(expected.getHeaderName())))
				.switchIfEmpty(tokenFromMultipartData(exchange, expected))
				.map((actual) -> equalsConstantTime(actual, expected.getToken()));
	}

	private Mono<String> tokenFromMultipartData(ServerWebExchange exchange, CsrfToken expected) {
		if (!this.isTokenFromMultipartDataEnabled) {
			return Mono.empty();
		}
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();
		MediaType contentType = headers.getContentType();
		if (!contentType.includes(MediaType.MULTIPART_FORM_DATA)) {
			return Mono.empty();
		}
		return exchange.getMultipartData().map((d) -> d.getFirst(expected.getParameterName())).cast(FormFieldPart.class)
				.map(FormFieldPart::value);
	}

	private Mono<Void> continueFilterChain(ServerWebExchange exchange, WebFilterChain chain) {
		return Mono.defer(() -> {
			Mono<CsrfToken> csrfToken = csrfToken(exchange);
			exchange.getAttributes().put(CsrfToken.class.getName(), csrfToken);
			return chain.filter(exchange);
		});
	}

	private Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
		return this.csrfTokenRepository.loadToken(exchange).switchIfEmpty(generateToken(exchange));
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

	private Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
		return this.csrfTokenRepository.generateToken(exchange)
				.delayUntil((token) -> this.csrfTokenRepository.saveToken(exchange, token));
	}

	private static class DefaultRequireCsrfProtectionMatcher implements ServerWebExchangeMatcher {

		private static final Set<HttpMethod> ALLOWED_METHODS = new HashSet<>(
				Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.OPTIONS));

		@Override
		public Mono<MatchResult> matches(ServerWebExchange exchange) {
			return Mono.just(exchange.getRequest()).flatMap((r) -> Mono.justOrEmpty(r.getMethod()))
					.filter(ALLOWED_METHODS::contains).flatMap((m) -> MatchResult.notMatch())
					.switchIfEmpty(MatchResult.match());
		}

	}

}
