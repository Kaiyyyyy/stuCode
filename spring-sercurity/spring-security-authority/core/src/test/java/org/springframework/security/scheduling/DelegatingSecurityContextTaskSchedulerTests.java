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

package org.springframework.security.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

/**
 * Test An implementation of {@link TaskScheduler} invoking it whenever the trigger
 * indicates a next execution time.
 *
 * @author Richard Valdivieso
 * @since 5.1
 */
public class DelegatingSecurityContextTaskSchedulerTests {

	@Mock
	private TaskScheduler scheduler;

	@Mock
	private Runnable runnable;

	@Mock
	private Trigger trigger;

	private DelegatingSecurityContextTaskScheduler delegatingSecurityContextTaskScheduler;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.delegatingSecurityContextTaskScheduler = new DelegatingSecurityContextTaskScheduler(this.scheduler);
	}

	@After
	public void cleanup() {
		this.delegatingSecurityContextTaskScheduler = null;
	}

	@Test
	public void testSchedulerIsNotNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingSecurityContextTaskScheduler(null));
	}

	@Test
	public void testSchedulerWithRunnableAndTrigger() {
		this.delegatingSecurityContextTaskScheduler.schedule(this.runnable, this.trigger);
		verify(this.scheduler).schedule(any(Runnable.class), any(Trigger.class));
	}

	@Test
	public void testSchedulerWithRunnableAndInstant() {
		Instant date = Instant.now();
		this.delegatingSecurityContextTaskScheduler.schedule(this.runnable, date);
		verify(this.scheduler).schedule(any(Runnable.class), any(Date.class));
	}

	@Test
	public void testScheduleAtFixedRateWithRunnableAndDate() {
		Date date = new Date(1544751374L);
		Duration duration = Duration.ofSeconds(4L);
		this.delegatingSecurityContextTaskScheduler.scheduleAtFixedRate(this.runnable, date, 1000L);
		verify(this.scheduler).scheduleAtFixedRate(isA(Runnable.class), isA(Date.class), eq(1000L));
	}

	@Test
	public void testScheduleAtFixedRateWithRunnableAndLong() {
		this.delegatingSecurityContextTaskScheduler.scheduleAtFixedRate(this.runnable, 1000L);
		verify(this.scheduler).scheduleAtFixedRate(isA(Runnable.class), eq(1000L));
	}

}
