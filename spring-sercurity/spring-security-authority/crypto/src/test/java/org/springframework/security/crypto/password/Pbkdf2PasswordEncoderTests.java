/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.security.crypto.password;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.KeyGenerators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class Pbkdf2PasswordEncoderTests {

	private Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder("secret");

	private Pbkdf2PasswordEncoder encoderSalt16 = new Pbkdf2PasswordEncoder("", 16);

	private Pbkdf2PasswordEncoder[] encoders = new Pbkdf2PasswordEncoder[] { this.encoder, this.encoderSalt16 };

	@Test
	public void encodedLengthSuccess() {
		// encode output is an hex coded String so with 2 chars per encoding result byte
		// (ie. 1 char for 4 bits).
		// The encoding result size is : (saltLength * 8) bits + hashWith bits.
		assertThat(this.encoder.encode("password").length()).isEqualTo((8 * 8 + 256) / 4);
		assertThat(this.encoderSalt16.encode("password").length()).isEqualTo((16 * 8 + 256) / 4);
	}

	@Test
	public void matches() {
		String result = this.encoder.encode("password");
		assertThat(result.equals("password")).isFalse();
		assertThat(this.encoder.matches("password", result)).isTrue();
	}

	@Test
	public void matchesWhenCustomSaltLengthThenSuccess() {
		String result = this.encoderSalt16.encode("password");
		assertThat(result.equals("password")).isFalse();
		assertThat(this.encoderSalt16.matches("password", result)).isTrue();
	}

	@Test
	public void matchesLengthChecked() {
		String result = this.encoder.encode("password");
		assertThat(this.encoder.matches("password", result.substring(0, result.length() - 2))).isFalse();
	}

	@Test
	public void matchesLengthCheckedWhenCustomSaltLengthThenSuccess() {
		String result = this.encoderSalt16.encode("password");
		assertThat(this.encoderSalt16.matches("password", result.substring(0, result.length() - 2))).isFalse();
	}

	@Test
	public void notMatches() {
		String result = this.encoder.encode("password");
		assertThat(this.encoder.matches("bogus", result)).isFalse();
	}

	@Test
	public void notMatchesWhenCustomSaltLengthThenSuccess() {
		String result = this.encoderSalt16.encode("password");
		assertThat(this.encoderSalt16.matches("bogus", result)).isFalse();
	}

	@Test
	public void encodeSamePasswordMultipleTimesDiffers() {
		String password = "password";
		String encodeFirst = this.encoder.encode(password);
		String encodeSecond = this.encoder.encode(password);
		assertThat(encodeFirst).isNotEqualTo(encodeSecond);
	}

	@Test
	public void encodeSamePasswordMultipleTimesWhenCustomSaltLengthThenDiffers() {
		String password = "password";
		String encodeFirst = this.encoderSalt16.encode(password);
		String encodeSecond = this.encoderSalt16.encode(password);
		assertThat(encodeFirst).isNotEqualTo(encodeSecond);
	}

	@Test
	public void passivity() {
		String encodedPassword = "ab1146a8458d4ce4e65789e5a3f60e423373cfa10b01abd23739e5ae2fdc37f8e9ede4ae6da65264";
		String rawPassword = "password";
		assertThat(this.encoder.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void passivityWhenCustomSaltLengthThenSuccess() {
		String encodedPassword = "0123456789abcdef0123456789abcdef10d883c2a0e653c97175c4a2583a7f1fd3301b319a7657d95f75365ea7c04ce1";
		String rawPassword = "password";
		assertThat(this.encoderSalt16.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void migrate() {
		final int saltLength = KeyGenerators.secureRandom().getKeyLength();
		String encodedPassword = "ab1146a8458d4ce4e65789e5a3f60e423373cfa10b01abd23739e5ae2fdc37f8e9ede4ae6da65264";
		String originalEncodedPassword = "ab1146a8458d4ce4ab1146a8458d4ce4e65789e5a3f60e423373cfa10b01abd23739e5ae2fdc37f8e9ede4ae6da65264";
		byte[] originalBytes = Hex.decode(originalEncodedPassword);
		byte[] fixedBytes = Arrays.copyOfRange(originalBytes, saltLength, originalBytes.length);
		String fixedHex = String.valueOf(Hex.encode(fixedBytes));
		assertThat(fixedHex).isEqualTo(encodedPassword);
	}

	@Test
	public void encodeAndMatchWhenBase64ThenSuccess() {
		this.encoder.setEncodeHashAsBase64(true);
		String rawPassword = "password";
		String encodedPassword = this.encoder.encode(rawPassword);
		assertThat(this.encoder.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void encodeAndMatchWhenBase64AndCustomSaltLengthThenSuccess() {
		this.encoderSalt16.setEncodeHashAsBase64(true);
		String rawPassword = "password";
		String encodedPassword = this.encoderSalt16.encode(rawPassword);
		assertThat(this.encoderSalt16.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void encodeWhenBase64ThenBase64DecodeSuccess() {
		assertThat(this.encoders).allSatisfy((pe) -> {
			pe.setEncodeHashAsBase64(true);
			String encodedPassword = pe.encode("password");
			// validate can decode as Base64
			assertThatNoException().isThrownBy(() -> java.util.Base64.getDecoder().decode(encodedPassword));
		});
	}

	@Test
	public void matchWhenBase64ThenSuccess() {
		this.encoder.setEncodeHashAsBase64(true);
		String rawPassword = "password";
		String encodedPassword = "3FOwOMcDgxP+z1x/sv184LFY2WVD+ZGMgYP3LPOSmCcDmk1XPYvcCQ==";
		assertThat(this.encoder.matches(rawPassword, encodedPassword)).isTrue();
		java.util.Base64.getDecoder().decode(encodedPassword); // validate can decode as
																// Base64
	}

	@Test
	public void matchWhenBase64AndCustomSaltLengthThenSuccess() {
		this.encoderSalt16.setEncodeHashAsBase64(true);
		String rawPassword = "password";
		String encodedPassword = "ASNFZ4mrze8BI0VniavN7xDYg8Kg5lPJcXXEolg6fx/TMBsxmnZX2V91Nl6nwEzh";
		assertThat(this.encoderSalt16.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void encodeAndMatchWhenSha256ThenSuccess() {
		this.encoder.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
		String rawPassword = "password";
		String encodedPassword = this.encoder.encode(rawPassword);
		assertThat(this.encoder.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void encodeAndMatchWhenSha256AndCustomSaltLengthThenSuccess() {
		this.encoderSalt16.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
		String rawPassword = "password";
		String encodedPassword = this.encoderSalt16.encode(rawPassword);
		assertThat(this.encoderSalt16.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void matchWhenSha256ThenSuccess() {
		this.encoder.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
		String rawPassword = "password";
		String encodedPassword = "821447f994e2b04c5014e31fa9fca4ae1cc9f2188c4ed53d3ddb5ba7980982b51a0ecebfc0b81a79";
		assertThat(this.encoder.matches(rawPassword, encodedPassword)).isTrue();
	}

	@Test
	public void matchWhenSha256AndCustomSaltLengthThenSuccess() {
		this.encoderSalt16.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
		String rawPassword = "password";
		String encodedPassword = "0123456789abcdef0123456789abcdefc7cfc96cd26b854d096ccbb3308fad860d719eb552ed52ef8352935539158287";
		assertThat(this.encoderSalt16.matches(rawPassword, encodedPassword)).isTrue();
	}

	/**
	 * Used to find the iteration count that takes .5 seconds.
	 */
	public void findDefaultIterationCount() {
		// warm up
		run(180000, 10);
		// find the default
		run(165000, 10);
	}

	private void run(int iterations, int count) {
		long HALF_SECOND = 500L;
		long avg = 0;
		while (avg < HALF_SECOND) {
			iterations += 10000;
			Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder("", iterations, 256);
			String encoded = encoder.encode("password");
			System.out.println("Trying " + iterations);
			long start = System.currentTimeMillis();
			for (int i = 0; i < count; i++) {
				encoder.matches("password", encoded);
			}
			long end = System.currentTimeMillis();
			long diff = end - start;
			avg = diff / count;
			System.out.println("Avgerage " + avg);
		}
		System.out.println("Iterations " + iterations);
	}

}
