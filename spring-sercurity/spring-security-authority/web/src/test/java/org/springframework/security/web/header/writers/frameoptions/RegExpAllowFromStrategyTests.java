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

package org.springframework.security.web.header.writers.frameoptions;

import java.util.regex.PatternSyntaxException;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Marten Deinum
 */
public class RegExpAllowFromStrategyTests {

	@Test
	public void invalidRegularExpressionShouldLeadToException() {
		assertThatExceptionOfType(PatternSyntaxException.class).isThrownBy(() -> new RegExpAllowFromStrategy("[a-z"));
	}

	@Test
	public void nullRegularExpressionShouldLeadToException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RegExpAllowFromStrategy(null));
	}

	@Test
	public void subdomainMatchingRegularExpression() {
		RegExpAllowFromStrategy strategy = new RegExpAllowFromStrategy("^https://([a-z0-9]*?\\.)test\\.com");
		strategy.setAllowFromParameterName("from");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("from", "https://www.test.com");
		String result1 = strategy.getAllowFromValue(request);
		assertThat(result1).isEqualTo("https://www.test.com");
		request.setParameter("from", "https://www.test.com");
		String result2 = strategy.getAllowFromValue(request);
		assertThat(result2).isEqualTo("https://www.test.com");
		request.setParameter("from", "https://test.foobar.com");
		String result3 = strategy.getAllowFromValue(request);
		assertThat(result3).isEqualTo("DENY");
	}

	@Test
	public void noParameterShouldDeny() {
		RegExpAllowFromStrategy strategy = new RegExpAllowFromStrategy("^https://([a-z0-9]*?\\.)test\\.com");
		MockHttpServletRequest request = new MockHttpServletRequest();
		String result1 = strategy.getAllowFromValue(request);
		assertThat(result1).isEqualTo("DENY");
	}

}
