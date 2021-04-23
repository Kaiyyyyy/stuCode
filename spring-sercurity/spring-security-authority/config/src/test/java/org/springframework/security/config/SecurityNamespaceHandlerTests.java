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

package org.springframework.security.config;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.messaging.Message;
import org.springframework.security.config.util.InMemoryXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Luke Taylor
 * @author Rob Winch
 * @since 3.0
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ClassUtils.class })
@PowerMockIgnore({ "org.w3c.dom.*", "org.xml.sax.*", "org.apache.xerces.*", "javax.xml.parsers.*" })
public class SecurityNamespaceHandlerTests {

	// @formatter:off
	private static final String XML_AUTHENTICATION_MANAGER = "<authentication-manager>"
			+ "  <authentication-provider>"
			+ "    <user-service id='us'>"
			+ "      <user name='bob' password='bobspassword' authorities='ROLE_A' />"
			+ "    </user-service>"
			+ "  </authentication-provider>"
			+ "</authentication-manager>";
	// @formatter:on

	private static final String XML_HTTP_BLOCK = "<http auto-config='true'/>";

	private static final String FILTER_CHAIN_PROXY_CLASSNAME = "org.springframework.security.web.FilterChainProxy";

	@Test
	public void constructionSucceeds() {
		new SecurityNamespaceHandler();
		// Shameless class coverage stats boosting
		new BeanIds() {
		};
		new Elements() {
		};
	}

	@Test
	public void pre32SchemaAreNotSupported() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new InMemoryXmlApplicationContext(
						"<user-service id='us'><user name='bob' password='bobspassword' authorities='ROLE_A' /></user-service>",
						"3.0.3", null))
				.withMessageContaining("You cannot use a spring-security-2.0.xsd");
	}

	// SEC-1868
	@Test
	public void initDoesNotLogErrorWhenFilterChainProxyFailsToLoad() throws Exception {
		String className = "javax.servlet.Filter";
		PowerMockito.spy(ClassUtils.class);
		PowerMockito.doThrow(new NoClassDefFoundError(className)).when(ClassUtils.class, "forName",
				eq(FILTER_CHAIN_PROXY_CLASSNAME), any(ClassLoader.class));
		Log logger = mock(Log.class);
		SecurityNamespaceHandler handler = new SecurityNamespaceHandler();
		ReflectionTestUtils.setField(handler, "logger", logger);
		handler.init();
		PowerMockito.verifyStatic(ClassUtils.class);
		ClassUtils.forName(eq(FILTER_CHAIN_PROXY_CLASSNAME), any(ClassLoader.class));
		verifyZeroInteractions(logger);
	}

	@Test
	public void filterNoClassDefFoundError() throws Exception {
		String className = "javax.servlet.Filter";
		PowerMockito.spy(ClassUtils.class);
		PowerMockito.doThrow(new NoClassDefFoundError(className)).when(ClassUtils.class, "forName",
				eq(FILTER_CHAIN_PROXY_CLASSNAME), any(ClassLoader.class));
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new InMemoryXmlApplicationContext(XML_AUTHENTICATION_MANAGER + XML_HTTP_BLOCK))
				.withMessageContaining("NoClassDefFoundError: " + className);
	}

	@Test
	public void filterNoClassDefFoundErrorNoHttpBlock() throws Exception {
		String className = "javax.servlet.Filter";
		PowerMockito.spy(ClassUtils.class);
		PowerMockito.doThrow(new NoClassDefFoundError(className)).when(ClassUtils.class, "forName",
				eq(FILTER_CHAIN_PROXY_CLASSNAME), any(ClassLoader.class));
		new InMemoryXmlApplicationContext(XML_AUTHENTICATION_MANAGER);
		// should load just fine since no http block
	}

	@Test
	public void filterChainProxyClassNotFoundException() throws Exception {
		String className = FILTER_CHAIN_PROXY_CLASSNAME;
		PowerMockito.spy(ClassUtils.class);
		PowerMockito.doThrow(new ClassNotFoundException(className)).when(ClassUtils.class, "forName",
				eq(FILTER_CHAIN_PROXY_CLASSNAME), any(ClassLoader.class));
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new InMemoryXmlApplicationContext(XML_AUTHENTICATION_MANAGER + XML_HTTP_BLOCK))
				.withMessageContaining("ClassNotFoundException: " + className);
	}

	@Test
	public void filterChainProxyClassNotFoundExceptionNoHttpBlock() throws Exception {
		String className = FILTER_CHAIN_PROXY_CLASSNAME;
		PowerMockito.spy(ClassUtils.class);
		PowerMockito.doThrow(new ClassNotFoundException(className)).when(ClassUtils.class, "forName",
				eq(FILTER_CHAIN_PROXY_CLASSNAME), any(ClassLoader.class));
		new InMemoryXmlApplicationContext(XML_AUTHENTICATION_MANAGER);
		// should load just fine since no http block
	}

	@Test
	public void websocketNotFoundExceptionNoMessageBlock() throws Exception {
		String className = FILTER_CHAIN_PROXY_CLASSNAME;
		PowerMockito.spy(ClassUtils.class);
		PowerMockito.doThrow(new ClassNotFoundException(className)).when(ClassUtils.class, "forName",
				eq(Message.class.getName()), any(ClassLoader.class));
		new InMemoryXmlApplicationContext(XML_AUTHENTICATION_MANAGER);
		// should load just fine since no websocket block
	}

}
