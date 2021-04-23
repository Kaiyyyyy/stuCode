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

package org.springframework.security.config.annotation.method.configuration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.intercept.AfterInvocationManager;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.access.intercept.RunAsManagerImpl;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityMetadataSourceAdvisor;
import org.springframework.security.access.intercept.aspectj.AspectJMethodSecurityInterceptor;
import org.springframework.security.access.method.AbstractMethodSecurityMetadataSource;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.authentication.AuthenticationManager;
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
public class NamespaceGlobalMethodSecurityTests {

	@Rule
	public final SpringTestRule spring = new SpringTestRule();

	@Autowired(required = false)
	private MethodSecurityService service;

	@Test
	@WithMockUser
	public void methodSecurityWhenCustomAccessDecisionManagerThenAuthorizes() {
		this.spring.register(CustomAccessDecisionManagerConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.secured());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenCustomAfterInvocationManagerThenAuthorizes() {
		this.spring.register(CustomAfterInvocationManagerConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorizePermitAll());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenCustomAuthenticationManagerThenAuthorizes() {
		this.spring.register(CustomAuthenticationConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> this.service.preAuthorize());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenJsr250EnabledThenAuthorizes() {
		this.spring.register(Jsr250Config.class, MethodSecurityServiceConfig.class).autowire();
		this.service.preAuthorize();
		this.service.secured();
		this.service.jsr250PermitAll();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.jsr250());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenCustomMethodSecurityMetadataSourceThenAuthorizes() {
		this.spring.register(CustomMethodSecurityMetadataSourceConfig.class, MethodSecurityServiceConfig.class)
				.autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.secured());
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.jsr250());
	}

	@Test
	@WithMockUser
	public void contextRefreshWhenUsingAspectJThenAutowire() throws Exception {
		this.spring.register(AspectJModeConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.spring.getContext().getBean(
				Class.forName("org.springframework.security.access.intercept.aspectj.aspect.AnnotationSecurityAspect")))
						.isNotNull();
		assertThat(this.spring.getContext().getBean(AspectJMethodSecurityInterceptor.class)).isNotNull();
		// TODO diagnose why aspectj isn't weaving method security advice around
		// MethodSecurityServiceImpl
	}

	@Test
	public void contextRefreshWhenUsingAspectJAndCustomGlobalMethodSecurityConfigurationThenAutowire()
			throws Exception {
		this.spring.register(AspectJModeExtendsGMSCConfig.class).autowire();
		assertThat(this.spring.getContext().getBean(
				Class.forName("org.springframework.security.access.intercept.aspectj.aspect.AnnotationSecurityAspect")))
						.isNotNull();
		assertThat(this.spring.getContext().getBean(AspectJMethodSecurityInterceptor.class)).isNotNull();
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenOrderSpecifiedThenConfigured() {
		this.spring.register(CustomOrderConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.spring.getContext().getBean("metaDataSourceAdvisor", MethodSecurityMetadataSourceAdvisor.class)
				.getOrder()).isEqualTo(-135);
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.jsr250());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenOrderUnspecifiedThenConfiguredToLowestPrecedence() {
		this.spring.register(DefaultOrderConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.spring.getContext().getBean("metaDataSourceAdvisor", MethodSecurityMetadataSourceAdvisor.class)
				.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> this.service.jsr250());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenOrderUnspecifiedAndCustomGlobalMethodSecurityConfigurationThenConfiguredToLowestPrecedence() {
		this.spring.register(DefaultOrderExtendsMethodSecurityConfig.class, MethodSecurityServiceConfig.class)
				.autowire();
		assertThat(this.spring.getContext().getBean("metaDataSourceAdvisor", MethodSecurityMetadataSourceAdvisor.class)
				.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> this.service.jsr250());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenPrePostEnabledThenPreAuthorizes() {
		this.spring.register(PreAuthorizeConfig.class, MethodSecurityServiceConfig.class).autowire();
		this.service.secured();
		this.service.jsr250();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenPrePostEnabledAndCustomGlobalMethodSecurityConfigurationThenPreAuthorizes() {
		this.spring.register(PreAuthorizeExtendsGMSCConfig.class, MethodSecurityServiceConfig.class).autowire();
		this.service.secured();
		this.service.jsr250();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenProxyTargetClassThenDoesNotWireToInterface() {
		this.spring.register(ProxyTargetClassConfig.class, MethodSecurityServiceConfig.class).autowire();
		// make sure service was actually proxied
		assertThat(this.service.getClass().getInterfaces()).doesNotContain(MethodSecurityService.class);
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenDefaultProxyThenWiresToInterface() {
		this.spring.register(DefaultProxyConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.service.getClass().getInterfaces()).contains(MethodSecurityService.class);
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenCustomRunAsManagerThenRunAsWrapsAuthentication() {
		this.spring.register(CustomRunAsManagerConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThat(this.service.runAs().getAuthorities())
				.anyMatch((authority) -> "ROLE_RUN_AS_SUPER".equals(authority.getAuthority()));
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenSecuredEnabledThenSecures() {
		this.spring.register(SecuredConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.secured());
		this.service.securedUser();
		this.service.preAuthorize();
		this.service.jsr250();
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenMissingEnableAnnotationThenShowsHelpfulError() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> this.spring.register(ExtendsNoEnableAnntotationConfig.class).autowire())
				.withStackTraceContaining(EnableGlobalMethodSecurity.class.getName() + " is required");
	}

	@Test
	@WithMockUser
	public void methodSecurityWhenImportingGlobalMethodSecurityConfigurationSubclassThenAuthorizes() {
		this.spring.register(ImportSubclassGMSCConfig.class, MethodSecurityServiceConfig.class).autowire();
		this.service.secured();
		this.service.jsr250();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> this.service.preAuthorize());
	}

	@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
	public static class CustomAccessDecisionManagerConfig extends GlobalMethodSecurityConfiguration {

		@Override
		protected AccessDecisionManager accessDecisionManager() {
			return new DenyAllAccessDecisionManager();
		}

		public static class DenyAllAccessDecisionManager implements AccessDecisionManager {

			@Override
			public void decide(Authentication authentication, Object object,
					Collection<ConfigAttribute> configAttributes) {
				throw new AccessDeniedException("Always Denied");
			}

			@Override
			public boolean supports(ConfigAttribute attribute) {
				return true;
			}

			@Override
			public boolean supports(Class<?> clazz) {
				return true;
			}

		}

	}

	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class CustomAfterInvocationManagerConfig extends GlobalMethodSecurityConfiguration {

		@Override
		protected AfterInvocationManager afterInvocationManager() {
			return new AfterInvocationManagerStub();
		}

		public static class AfterInvocationManagerStub implements AfterInvocationManager {

			@Override
			public Object decide(Authentication authentication, Object object, Collection<ConfigAttribute> attributes,
					Object returnedObject) throws AccessDeniedException {
				throw new AccessDeniedException("custom AfterInvocationManager");
			}

			@Override
			public boolean supports(ConfigAttribute attribute) {
				return true;
			}

			@Override
			public boolean supports(Class<?> clazz) {
				return true;
			}

		}

	}

	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class CustomAuthenticationConfig extends GlobalMethodSecurityConfiguration {

		@Override
		public MethodInterceptor methodSecurityInterceptor(MethodSecurityMetadataSource methodSecurityMetadataSource) {
			MethodInterceptor interceptor = super.methodSecurityInterceptor(methodSecurityMetadataSource);
			((MethodSecurityInterceptor) interceptor).setAlwaysReauthenticate(true);
			return interceptor;
		}

		@Override
		protected AuthenticationManager authenticationManager() {
			return (authentication) -> {
				throw new UnsupportedOperationException();
			};
		}

	}

	@EnableGlobalMethodSecurity(jsr250Enabled = true)
	@Configuration
	public static class Jsr250Config {

	}

	@EnableGlobalMethodSecurity
	public static class CustomMethodSecurityMetadataSourceConfig extends GlobalMethodSecurityConfiguration {

		@Override
		protected MethodSecurityMetadataSource customMethodSecurityMetadataSource() {
			return new AbstractMethodSecurityMetadataSource() {
				@Override
				public Collection<ConfigAttribute> getAttributes(Method method, Class<?> targetClass) {
					// require ROLE_NOBODY for any method on MethodSecurityService
					// interface
					return MethodSecurityService.class.isAssignableFrom(targetClass)
							? Arrays.asList(new SecurityConfig("ROLE_NOBODY")) : Collections.emptyList();
				}

				@Override
				public Collection<ConfigAttribute> getAllConfigAttributes() {
					return null;
				}
			};
		}

	}

	@EnableGlobalMethodSecurity(mode = AdviceMode.ASPECTJ, securedEnabled = true)
	public static class AspectJModeConfig {

	}

	@EnableGlobalMethodSecurity(mode = AdviceMode.ASPECTJ, securedEnabled = true)
	public static class AspectJModeExtendsGMSCConfig extends GlobalMethodSecurityConfiguration {

	}

	private static class AdvisorOrderConfig implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder advice = BeanDefinitionBuilder.rootBeanDefinition(ExceptingInterceptor.class);
			registry.registerBeanDefinition("exceptingInterceptor", advice.getBeanDefinition());
			BeanDefinitionBuilder advisor = BeanDefinitionBuilder
					.rootBeanDefinition(MethodSecurityMetadataSourceAdvisor.class);
			advisor.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			advisor.addConstructorArgValue("exceptingInterceptor");
			advisor.addConstructorArgReference("methodSecurityMetadataSource");
			advisor.addConstructorArgValue("methodSecurityMetadataSource");
			advisor.addPropertyValue("order", 0);
			registry.registerBeanDefinition("exceptingAdvisor", advisor.getBeanDefinition());
		}

		private static class ExceptingInterceptor implements MethodInterceptor {

			@Override
			public Object invoke(MethodInvocation invocation) {
				throw new UnsupportedOperationException("Deny All");
			}

		}

	}

	@EnableGlobalMethodSecurity(order = -135, jsr250Enabled = true)
	@Import(AdvisorOrderConfig.class)
	public static class CustomOrderConfig {

	}

	@EnableGlobalMethodSecurity(jsr250Enabled = true)
	@Import(AdvisorOrderConfig.class)
	public static class DefaultOrderConfig {

	}

	@EnableGlobalMethodSecurity(jsr250Enabled = true)
	@Import(AdvisorOrderConfig.class)
	public static class DefaultOrderExtendsMethodSecurityConfig extends GlobalMethodSecurityConfiguration {

	}

	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class PreAuthorizeConfig {

	}

	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class PreAuthorizeExtendsGMSCConfig extends GlobalMethodSecurityConfiguration {

	}

	@EnableGlobalMethodSecurity(proxyTargetClass = true, prePostEnabled = true)
	public static class ProxyTargetClassConfig {

	}

	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class DefaultProxyConfig {

	}

	@EnableGlobalMethodSecurity(securedEnabled = true)
	public static class CustomRunAsManagerConfig extends GlobalMethodSecurityConfiguration {

		@Override
		protected RunAsManager runAsManager() {
			RunAsManagerImpl runAsManager = new RunAsManagerImpl();
			runAsManager.setKey("some key");
			return runAsManager;
		}

	}

	@EnableGlobalMethodSecurity(securedEnabled = true)
	public static class SecuredConfig {

	}

	@Configuration
	public static class ExtendsNoEnableAnntotationConfig extends GlobalMethodSecurityConfiguration {

	}

	@Configuration
	@Import(PreAuthorizeExtendsGMSCConfig.class)
	public static class ImportSubclassGMSCConfig {

	}

}
