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

import java.util.List;

import org.springframework.core.log.LogMessage;
import org.springframework.messaging.Message;

/**
 * {@link MessageMatcher} that will return true if any of the passed in
 * {@link MessageMatcher} instances match.
 *
 * @since 4.0
 */
public final class OrMessageMatcher<T> extends AbstractMessageMatcherComposite<T> {

	/**
	 * Creates a new instance
	 * @param messageMatchers the {@link MessageMatcher} instances to try
	 */
	public OrMessageMatcher(List<MessageMatcher<T>> messageMatchers) {
		super(messageMatchers);
	}

	/**
	 * Creates a new instance
	 * @param messageMatchers the {@link MessageMatcher} instances to try
	 */
	@SafeVarargs
	public OrMessageMatcher(MessageMatcher<T>... messageMatchers) {
		super(messageMatchers);

	}

	@Override
	public boolean matches(Message<? extends T> message) {
		for (MessageMatcher<T> matcher : getMessageMatchers()) {
			this.logger.debug(LogMessage.format("Trying to match using %s", matcher));
			if (matcher.matches(message)) {
				this.logger.debug("matched");
				return true;
			}
		}
		this.logger.debug("No matches found");
		return false;
	}

}
