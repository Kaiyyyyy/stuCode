/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.security.config.annotation.method.configuration;

import java.io.Serializable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.test.SpringTestRule;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.annotation.SecurityTestExecutionListeners;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Winch
 * @author Josh Cummings
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SecurityTestExecutionListeners
public class NamespaceGlobalMethodSecurityExpressionHandlerTests {

	@Rule
	public final SpringTestRule spring = new SpringTestRule();

	@Autowired(required = false)
	private MethodSecurityService service;

	@Test
	@WithMockUser
	public void methodSecurityWhenUsingCustomPermissionEvaluatorThenPreAuthorizesAccordingly() {
		this.spring.register(CustomAccessDecisionManagerConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.service.hasPermission("granted")).isNull();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.hasPermission("denied"));
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenUsingCustomPermissionEvaluatorThenPostAuthorizesAccordingly() {
		this.spring.register(CustomAccessDecisionManagerConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.service.postHasPermission("granted")).isNull();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.service.postHasPermission("denied"));
	}

	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class CustomAccessDecisionManagerConfig extends GlobalMethodSecurityConfiguration {

		@Override
		protected MethodSecurityExpressionHandler createExpressionHandler() {
			DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
			expressionHandler.setPermissionEvaluator(new PermissionEvaluator() {
				@Override
				public boolean hasPermission(Authentication authentication, Object targetDomainObject,
						Object permission) {
					return "granted".equals(targetDomainObject);
				}

				@Override
				public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
						Object permission) {
					throw new UnsupportedOperationException();
				}
			});
			return expressionHandler;
		}

	}

}
