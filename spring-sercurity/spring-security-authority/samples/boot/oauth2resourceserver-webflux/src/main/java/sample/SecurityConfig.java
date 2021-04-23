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

package sample;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author Rob Winch
 * @since 5.1
 */
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http
			.authorizeExchange((exchanges) -> 
				exchanges
					.pathMatchers(HttpMethod.GET, "/message/**").hasAuthority("SCOPE_message:read")
					.pathMatchers(HttpMethod.POST, "/message/**").hasAuthority("SCOPE_message:write")
					.anyExchange().authenticated()
			)
			.oauth2ResourceServer((oauth2ResourceServer) -> 
				oauth2ResourceServer
					.jwt(withDefaults())
			);
		return http.build();
	}
}
