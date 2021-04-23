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

package org.springframework.security.access.expression;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Luke Taylor
 */
public class AbstractSecurityExpressionHandlerTests {

	private AbstractSecurityExpressionHandler<Object> handler;

	@Before
	public void setUp() {
		this.handler = new AbstractSecurityExpressionHandler<Object>() {
			@Override
			protected SecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
					Object o) {
				return new SecurityExpressionRoot(authentication) {
				};
			}
		};
	}

	@Test
	public void beanNamesAreCorrectlyResolved() {
		this.handler.setApplicationContext(new AnnotationConfigApplicationContext(TestConfiguration.class));
		Expression expression = this.handler.getExpressionParser()
				.parseExpression("@number10.compareTo(@number20) < 0");
		assertThat(expression.getValue(this.handler.createEvaluationContext(mock(Authentication.class), new Object())))
				.isEqualTo(true);
	}

	@Test
	public void setExpressionParserNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.handler.setExpressionParser(null));
	}

	@Test
	public void setExpressionParser() {
		SpelExpressionParser parser = new SpelExpressionParser();
		this.handler.setExpressionParser(parser);
		assertThat(parser == this.handler.getExpressionParser()).isTrue();
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		Integer number10() {
			return 10;
		}

		@Bean
		Integer number20() {
			return 20;
		}

	}

}
