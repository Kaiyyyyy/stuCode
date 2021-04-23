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

package org.springframework.security.web.authentication.switchuser;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Clement Ng
 *
 */
public class SwitchUserGrantedAuthorityTests {

	@Test
	public void authorityWithNullRoleFailsAssertion() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SwitchUserGrantedAuthority(null, null))
				.withMessage("role cannot be null");
	}

	@Test
	public void authorityWithNullSourceFailsAssertion() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SwitchUserGrantedAuthority("role", null))
				.withMessage("source cannot be null");
	}

}
