/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.security.config.authentication;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.util.InMemoryXmlApplicationContext;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AuthenticationProviderBeanDefinitionParser}.
 *
 * @author Luke Taylor
 */
public class AuthenticationProviderBeanDefinitionParserTests {

	private AbstractXmlApplicationContext appContext;

	private UsernamePasswordAuthenticationToken bob = new UsernamePasswordAuthenticationToken("bob", "bobspassword");

	@After
	public void closeAppContext() {
		if (this.appContext != null) {
			this.appContext.close();
		}
	}

	@Test
	public void worksWithEmbeddedUserService() {
		// @formatter:off
		setContext(" <authentication-provider>"
				+ "        <user-service>"
				+ "            <user name='bob' password='{noop}bobspassword' authorities='ROLE_A' />"
				+ "        </user-service>"
				+ "    </authentication-provider>");
		// @formatter:on
		getProvider().authenticate(this.bob);
	}

	@Test
	public void externalUserServiceRefWorks() {
		// @formatter:off
		this.appContext = new InMemoryXmlApplicationContext(
				"    <authentication-manager>"
				+ "        <authentication-provider user-service-ref='myUserService' />"
				+ "    </authentication-manager>" + "    <user-service id='myUserService'>"
				+ "       <user name='bob' password='{noop}bobspassword' authorities='ROLE_A' />"
				+ "    </user-service>");
		// @formatter:on
		getProvider().authenticate(this.bob);
	}

	@Test
	public void providerWithBCryptPasswordEncoderWorks() {
		// @formatter:off
		setContext(" <authentication-provider>"
				+ "        <password-encoder hash='bcrypt'/>"
				+ "        <user-service>"
				+ "            <user name='bob' password='$2a$05$dRmjl1T05J7rvCPD2NgsHesCEJHww3pdmesUhjM3PD4m/gaEYyx/G' authorities='ROLE_A' />"
				+ "        </user-service>"
		// @formatter:on
				+ "    </authentication-provider>");
		getProvider().authenticate(this.bob);
	}

	@Test
	public void providerWithMd5PasswordEncoderWorks() {
		// @formatter:off
		this.appContext = new InMemoryXmlApplicationContext(" <authentication-manager>"
				+ " <authentication-provider>"
				+ "        <password-encoder ref='passwordEncoder'/>"
				+ "        <user-service>"
				+ "            <user name='bob' password='12b141f35d58b8b3a46eea65e6ac179e' authorities='ROLE_A' />"
				+ "        </user-service>"
				+ "    </authentication-provider>"
				+ " </authentication-manager>"
				+ " <b:bean id='passwordEncoder'  class='" + MessageDigestPasswordEncoder.class.getName() + "'>"
				+ "     <b:constructor-arg value='MD5'/>"
				+ " </b:bean>");
		// @formatter:on
		getProvider().authenticate(this.bob);
	}

	@Test
	public void providerWithShaPasswordEncoderWorks() {
		// @formatter:off
		this.appContext = new InMemoryXmlApplicationContext(" <authentication-manager>"
				+ " <authentication-provider>"
				+ "        <password-encoder ref='passwordEncoder'/>"
				+ "        <user-service>"
				+ "            <user name='bob' password='{SSHA}PpuEwfdj7M1rs0C2W4ssSM2XEN/Y6S5U' authorities='ROLE_A' />"
				+ "        </user-service>"
				+ "    </authentication-provider>"
				+ " </authentication-manager>"
				+ " <b:bean id='passwordEncoder'  class='" + LdapShaPasswordEncoder.class.getName() + "'/>");
		// @formatter:on
		getProvider().authenticate(this.bob);
	}

	@Test
	public void passwordIsBase64EncodedWhenBase64IsEnabled() {
		// @formatter:off
		this.appContext = new InMemoryXmlApplicationContext(" <authentication-manager>"
				+ " <authentication-provider>"
				+ "        <password-encoder ref='passwordEncoder'/>"
				+ "        <user-service>"
				+ "            <user name='bob' password='ErFB811YuLOkbupl5qwXng==' authorities='ROLE_A' />"
				+ "        </user-service>"
				+ "    </authentication-provider>"
				+ " </authentication-manager>"
				+ " <b:bean id='passwordEncoder'  class='" + MessageDigestPasswordEncoder.class.getName() + "'>"
				+ "     <b:constructor-arg value='MD5'/>" + "     <b:property name='encodeHashAsBase64' value='true'/>"
				+ " </b:bean>");
		// @formatter:on
		getProvider().authenticate(this.bob);
	}

	// SEC-1466
	@Test
	public void exernalProviderDoesNotSupportChildElements() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
		// @formatter:off
			this.appContext = new InMemoryXmlApplicationContext("    <authentication-manager>"
					+ "      <authentication-provider ref='aProvider'> "
					+ "        <password-encoder ref='customPasswordEncoder'/>"
					+ "      </authentication-provider>"
					+ "    </authentication-manager>"
					+ "    <b:bean id='aProvider' class='org.springframework.security.authentication.TestingAuthenticationProvider'/>"
					+ "    <b:bean id='customPasswordEncoder' "
					+ "        class='org.springframework.security.authentication.encoding.Md5PasswordEncoder'/>")
		// @formatter:on
		);
	}

	private AuthenticationProvider getProvider() {
		List<AuthenticationProvider> providers = ((ProviderManager) this.appContext
				.getBean(BeanIds.AUTHENTICATION_MANAGER)).getProviders();
		return providers.get(0);
	}

	private void setContext(String context) {
		this.appContext = new InMemoryXmlApplicationContext(
				"<authentication-manager>" + context + "</authentication-manager>");
	}

}
