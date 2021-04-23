/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.security.web.server.ui;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.HtmlUtils;

/**
 * Generates a default log in page used for authenticating users.
 *
 * @author Rob Winch
 * @since 5.0
 */
public class LoginPageGeneratingWebFilter implements WebFilter {

	private ServerWebExchangeMatcher matcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/login");

	private Map<String, String> oauth2AuthenticationUrlToClientName = new HashMap<>();

	private boolean formLoginEnabled;

	public void setFormLoginEnabled(boolean enabled) {
		this.formLoginEnabled = enabled;
	}

	public void setOauth2AuthenticationUrlToClientName(Map<String, String> oauth2AuthenticationUrlToClientName) {
		Assert.notNull(oauth2AuthenticationUrlToClientName, "oauth2AuthenticationUrlToClientName cannot be null");
		this.oauth2AuthenticationUrlToClientName = oauth2AuthenticationUrlToClientName;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return this.matcher.matches(exchange).filter(ServerWebExchangeMatcher.MatchResult::isMatch)
				.switchIfEmpty(chain.filter(exchange).then(Mono.empty())).flatMap((matchResult) -> render(exchange));
	}

	private Mono<Void> render(ServerWebExchange exchange) {
		ServerHttpResponse result = exchange.getResponse();
		result.setStatusCode(HttpStatus.OK);
		result.getHeaders().setContentType(MediaType.TEXT_HTML);
		return result.writeWith(createBuffer(exchange));
	}

	private Mono<DataBuffer> createBuffer(ServerWebExchange exchange) {
		Mono<CsrfToken> token = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
		return token.map(LoginPageGeneratingWebFilter::csrfToken).defaultIfEmpty("").map((csrfTokenHtmlInput) -> {
			byte[] bytes = createPage(exchange, csrfTokenHtmlInput);
			DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
			return bufferFactory.wrap(bytes);
		});
	}

	private byte[] createPage(ServerWebExchange exchange, String csrfTokenHtmlInput) {
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		String contextPath = exchange.getRequest().getPath().contextPath().value();
		StringBuilder page = new StringBuilder();
		page.append("<!DOCTYPE html>\n");
		page.append("<html lang=\"en\">\n");
		page.append("  <head>\n");
		page.append("    <meta charset=\"utf-8\">\n");
		page.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n");
		page.append("    <meta name=\"description\" content=\"\">\n");
		page.append("    <meta name=\"author\" content=\"\">\n");
		page.append("    <title>Please sign in</title>\n");
		page.append("    <link href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css\" "
				+ "rel=\"stylesheet\" integrity=\"sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M\" "
				+ "crossorigin=\"anonymous\">\n");
		page.append("    <link href=\"https://getbootstrap.com/docs/4.0/examples/signin/signin.css\" "
				+ "rel=\"stylesheet\" crossorigin=\"anonymous\"/>\n");
		page.append("  </head>\n");
		page.append("  <body>\n");
		page.append("     <div class=\"container\">\n");
		page.append(formLogin(queryParams, contextPath, csrfTokenHtmlInput));
		page.append(oauth2LoginLinks(queryParams, contextPath, this.oauth2AuthenticationUrlToClientName));
		page.append("    </div>\n");
		page.append("  </body>\n");
		page.append("</html>");
		return page.toString().getBytes(Charset.defaultCharset());
	}

	private String formLogin(MultiValueMap<String, String> queryParams, String contextPath, String csrfTokenHtmlInput) {
		if (!this.formLoginEnabled) {
			return "";
		}
		boolean isError = queryParams.containsKey("error");
		boolean isLogoutSuccess = queryParams.containsKey("logout");
		StringBuilder page = new StringBuilder();
		page.append("      <form class=\"form-signin\" method=\"post\" action=\"" + contextPath + "/login\">\n");
		page.append("        <h2 class=\"form-signin-heading\">Please sign in</h2>\n");
		page.append(createError(isError));
		page.append(createLogoutSuccess(isLogoutSuccess));
		page.append("        <p>\n");
		page.append("          <label for=\"username\" class=\"sr-only\">Username</label>\n");
		page.append("          <input type=\"text\" id=\"username\" name=\"username\" "
				+ "class=\"form-control\" placeholder=\"Username\" required autofocus>\n");
		page.append("        </p>\n" + "        <p>\n");
		page.append("          <label for=\"password\" class=\"sr-only\">Password</label>\n");
		page.append("          <input type=\"password\" id=\"password\" name=\"password\" "
				+ "class=\"form-control\" placeholder=\"Password\" required>\n");
		page.append("        </p>\n");
		page.append(csrfTokenHtmlInput);
		page.append("        <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Sign in</button>\n");
		page.append("      </form>\n");
		return page.toString();
	}

	private static String oauth2LoginLinks(MultiValueMap<String, String> queryParams, String contextPath,
			Map<String, String> oauth2AuthenticationUrlToClientName) {
		if (oauth2AuthenticationUrlToClientName.isEmpty()) {
			return "";
		}
		boolean isError = queryParams.containsKey("error");
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"container\"><h2 class=\"form-signin-heading\">Login with OAuth 2.0</h2>");
		sb.append(createError(isError));
		sb.append("<table class=\"table table-striped\">\n");
		for (Map.Entry<String, String> clientAuthenticationUrlToClientName : oauth2AuthenticationUrlToClientName
				.entrySet()) {
			sb.append(" <tr><td>");
			String url = clientAuthenticationUrlToClientName.getKey();
			sb.append("<a href=\"").append(contextPath).append(url).append("\">");
			String clientName = HtmlUtils.htmlEscape(clientAuthenticationUrlToClientName.getValue());
			sb.append(clientName);
			sb.append("</a>");
			sb.append("</td></tr>\n");
		}
		sb.append("</table></div>\n");
		return sb.toString();
	}

	private static String csrfToken(CsrfToken token) {
		return "          <input type=\"hidden\" name=\"" + token.getParameterName() + "\" value=\"" + token.getToken()
				+ "\">\n";
	}

	private static String createError(boolean isError) {
		return isError ? "<div class=\"alert alert-danger\" role=\"alert\">Invalid credentials</div>" : "";
	}

	private static String createLogoutSuccess(boolean isLogoutSuccess) {
		return isLogoutSuccess ? "<div class=\"alert alert-success\" role=\"alert\">You have been signed out</div>"
				: "";
	}

}
