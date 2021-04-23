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

package org.springframework.security.messaging.util.matcher;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Abstract {@link MessageMatcher} containing multiple {@link MessageMatcher}
 *
 * @since 4.0
 */
public abstract class AbstractMessageMatcherComposite<T> implements MessageMatcher<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * @deprecated since 5.4 in favor of {@link #logger}
	 */
	@Deprecated
	protected final Log LOGGER = this.logger;

	private final List<MessageMatcher<T>> messageMatchers;

	/**
	 * Creates a new instance
	 * @param messageMatchers the {@link MessageMatcher} instances to try
	 */
	AbstractMessageMatcherComposite(List<MessageMatcher<T>> messageMatchers) {
		Assert.notEmpty(messageMatchers, "messageMatchers must contain a value");
		Assert.isTrue(!messageMatchers.contains(null), "messageMatchers cannot contain null values");
		this.messageMatchers = messageMatchers;

	}

	/**
	 * Creates a new instance
	 * @param messageMatchers the {@link MessageMatcher} instances to try
	 */
	@SafeVarargs
	AbstractMessageMatcherComposite(MessageMatcher<T>... messageMatchers) {
		this(Arrays.asList(messageMatchers));
	}

	public List<MessageMatcher<T>> getMessageMatchers() {
		return this.messageMatchers;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[messageMatchers=" + this.messageMatchers + "]";
	}

}
