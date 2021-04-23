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

package org.springframework.security.config.annotation.web.configurers;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.test.SpringTestRule;
import org.springframework.security.web.PortMapperImpl;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * @author Rob Winch
 * @author Josh Cummings
 */
public class PortMapperConfigurerTests {

	@Rule
	public final SpringTestRule spring = new SpringTestRule();

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void requestWhenPortMapperTwiceInvokedThenDoesNotOverride() throws Exception {
		this.spring.register(InvokeTwiceDoesNotOverride.class).autowire();
		this.mockMvc.perform(get("http://localhost:543")).andExpect(redirectedUrl("https://localhost:123"));
	}

	@Test
	public void requestWhenPortMapperHttpMapsToInLambdaThenRedirectsToHttpsPort() throws Exception {
		this.spring.register(HttpMapsToInLambdaConfig.class).autowire();
		this.mockMvc.perform(get("http://localhost:543")).andExpect(redirectedUrl("https://localhost:123"));
	}

	@Test
	public void requestWhenCustomPortMapperInLambdaThenRedirectsToHttpsPort() throws Exception {
		this.spring.register(CustomPortMapperInLambdaConfig.class).autowire();
		this.mockMvc.perform(get("http://localhost:543")).andExpect(redirectedUrl("https://localhost:123"));
	}

	@EnableWebSecurity
	static class InvokeTwiceDoesNotOverride extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.requiresChannel()
					.anyRequest().requiresSecure()
					.and()
				.portMapper()
					.http(543).mapsTo(123)
					.and()
				.portMapper();
			// @formatter:on
		}

	}

	@EnableWebSecurity
	static class HttpMapsToInLambdaConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.requiresChannel((requiresChannel) ->
					requiresChannel
					.anyRequest().requiresSecure()
				)
				.portMapper((portMapper) ->
					portMapper
						.http(543).mapsTo(123)
				);
			// @formatter:on
		}

	}

	@EnableWebSecurity
	static class CustomPortMapperInLambdaConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			PortMapperImpl customPortMapper = new PortMapperImpl();
			customPortMapper.setPortMappings(Collections.singletonMap("543", "123"));
			// @formatter:off
			http
				.requiresChannel((requiresChannel) ->
					requiresChannel
						.anyRequest().requiresSecure()
				)
				.portMapper((portMapper) ->
					portMapper
						.portMapper(customPortMapper)
				);
			// @formatter:on
		}

	}

}
