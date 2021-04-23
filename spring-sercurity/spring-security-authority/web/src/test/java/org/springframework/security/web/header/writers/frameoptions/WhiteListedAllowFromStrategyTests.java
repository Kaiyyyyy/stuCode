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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test for the {@code WhiteListedAllowFromStrategy}.
 *
 * @author Marten Deinum
 * @since 3.2
 */
public class WhiteListedAllowFromStrategyTests {

	@Test
	public void emptyListShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new WhiteListedAllowFromStrategy(new ArrayList<>()));
	}

	@Test
	public void nullListShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new WhiteListedAllowFromStrategy(null));
	}

	@Test
	public void listWithSingleElementShouldMatch() {
		List<String> allowed = new ArrayList<>();
		allowed.add("https://www.test.com");
		WhiteListedAllowFromStrategy strategy = new WhiteListedAllowFromStrategy(allowed);
		strategy.setAllowFromParameterName("from");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("from", "https://www.test.com");
		String result = strategy.getAllowFromValue(request);
		assertThat(result).isEqualTo("https://www.test.com");
	}

	@Test
	public void listWithMultipleElementShouldMatch() {
		List<String> allowed = new ArrayList<>();
		allowed.add("https://www.test.com");
		allowed.add("https://www.springsource.org");
		WhiteListedAllowFromStrategy strategy = new WhiteListedAllowFromStrategy(allowed);
		strategy.setAllowFromParameterName("from");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("from", "https://www.test.com");
		String result = strategy.getAllowFromValue(request);
		assertThat(result).isEqualTo("https://www.test.com");
	}

	@Test
	public void listWithSingleElementShouldNotMatch() {
		List<String> allowed = new ArrayList<>();
		allowed.add("https://www.test.com");
		WhiteListedAllowFromStrategy strategy = new WhiteListedAllowFromStrategy(allowed);
		strategy.setAllowFromParameterName("from");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("from", "https://www.test123.com");
		String result = strategy.getAllowFromValue(request);
		assertThat(result).isEqualTo("DENY");
	}

	@Test
	public void requestWithoutParameterShouldNotMatch() {
		List<String> allowed = new ArrayList<>();
		allowed.add("https://www.test.com");
		WhiteListedAllowFromStrategy strategy = new WhiteListedAllowFromStrategy(allowed);
		strategy.setAllowFromParameterName("from");
		MockHttpServletRequest request = new MockHttpServletRequest();
		String result = strategy.getAllowFromValue(request);
		assertThat(result).isEqualTo("DENY");
	}

}
