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

package org.springframework.security.samples.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.samples.filter.CustomAuthenticationFilter;
import org.springframework.security.samples.handle.CustomAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * @author Joe Grandja
 */
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private AuthenticationSuccessHandler customAuthenticationSuccessHandler;

	@Autowired
	private AuthenticationFailureHandler customAuthenticationFailureHandler;


	// @formatter:off
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter("/kaiy");
		http
				.addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeRequests((authorize) -> authorize
					.antMatchers("/css/**", "/index","/login","/loginkaiy","/login.html","/oauth/**").permitAll()
					.antMatchers("/user/**").hasRole("USER")
						.anyRequest().authenticated()
				)
				.formLogin((formLogin) -> formLogin
						.loginPage("/login")
						.loginProcessingUrl("/loginkaiy")
						.failureUrl("/login-error")
						//successHandler()、defaultSuccessUrl()排在前面的生效
						.successHandler(customAuthenticationSuccessHandler)
//						.defaultSuccessUrl("/defaultSuccessUrl")
						.failureHandler(customAuthenticationFailureHandler)

				);
	}
	// @formatter:on

//	@Bean
	public UserDetailsService userDetailsService() {
		UserDetails userDetails = User.withDefaultPasswordEncoder()
				.username("user")
				.password("password")
				.roles("USER")
				.build();
		return new InMemoryUserDetailsManager(userDetails);
	}

	@Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}
}
