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

package org.springframework.security.crypto.codec;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Luke Taylor
 */
@SuppressWarnings("deprecation")
public class Base64Tests {

	@Test
	public void isBase64ReturnsTrueForValidBase64() {
		assertThat(Base64.isBase64(new byte[] { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D' })).isTrue();
	}

	@Test
	public void isBase64ReturnsFalseForInvalidBase64() {
		// Include invalid '`' character
		assertThat(Base64.isBase64(new byte[] { (byte) 'A', (byte) 'B', (byte) 'C', (byte) '`' })).isFalse();
	}

	@Test
	public void isBase64RejectsNull() {
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> Base64.isBase64(null));
	}

	@Test
	public void isBase64RejectsInvalidLength() {
		assertThatIllegalArgumentException().isThrownBy(() -> Base64.isBase64(new byte[] { (byte) 'A' }));
	}

}
