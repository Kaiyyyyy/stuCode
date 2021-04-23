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

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.util.EncodingUtils;

/**
 * A {@link PasswordEncoder} implementation that uses PBKDF2 with :
 * <ul>
 * <li>a configurable random salt value length (default is {@value #DEFAULT_SALT_LENGTH}
 * bytes)</li>
 * <li>a configurable number of iterations (default is {@value #DEFAULT_ITERATIONS})</li>
 * <li>a configurable output hash width (default is {@value #DEFAULT_HASH_WIDTH}
 * bits)</li>
 * <li>a configurable key derivation function (see {@link SecretKeyFactoryAlgorithm})</li>
 * <li>a configurable secret appended to the random salt (default is empty)</li>
 * </ul>
 * The algorithm is invoked on the concatenated bytes of the salt, secret and password.
 *
 * @author Rob Worsnop
 * @author Rob Winch
 * @author Loïc Guibert
 * @since 4.1
 */
public class Pbkdf2PasswordEncoder implements PasswordEncoder {

	private static final int DEFAULT_SALT_LENGTH = 8;

	private static final int DEFAULT_HASH_WIDTH = 256;

	private static final int DEFAULT_ITERATIONS = 185000;

	private final BytesKeyGenerator saltGenerator;

	private final byte[] secret;

	private final int hashWidth;

	private final int iterations;

	private String algorithm = SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA1.name();

	private boolean encodeHashAsBase64;

	/**
	 * Constructs a PBKDF2 password encoder with no additional secret value. There will be
	 * a salt length of {@value #DEFAULT_SALT_LENGTH} bytes, {@value #DEFAULT_ITERATIONS}
	 * iterations and a hash width of {@value #DEFAULT_HASH_WIDTH} bits. The default is
	 * based upon aiming for .5 seconds to validate the password when this class was
	 * added. Users should tune password verification to their own systems.
	 */
	public Pbkdf2PasswordEncoder() {
		this("");
	}

	/**
	 * Constructs a standard password encoder with a secret value which is also included
	 * in the password hash. There will be a salt length of {@value #DEFAULT_SALT_LENGTH}
	 * bytes, {@value #DEFAULT_ITERATIONS} iterations and a hash width of
	 * {@value #DEFAULT_HASH_WIDTH} bits.
	 * @param secret the secret key used in the encoding process (should not be shared)
	 */
	public Pbkdf2PasswordEncoder(CharSequence secret) {
		this(secret, DEFAULT_SALT_LENGTH, DEFAULT_ITERATIONS, DEFAULT_HASH_WIDTH);
	}

	/**
	 * Constructs a standard password encoder with a secret value as well as salt length.
	 * There will be {@value #DEFAULT_ITERATIONS} iterations and a hash width of
	 * {@value #DEFAULT_HASH_WIDTH} bits.
	 * @param secret the secret
	 * @param saltLength the salt length (in bytes)
	 * @since 5.5
	 */
	public Pbkdf2PasswordEncoder(CharSequence secret, int saltLength) {
		this(secret, saltLength, DEFAULT_ITERATIONS, DEFAULT_HASH_WIDTH);
	}

	/**
	 * Constructs a standard password encoder with a secret value as well as iterations
	 * and hash width. The salt length will be of {@value #DEFAULT_SALT_LENGTH} bytes.
	 * @param secret the secret
	 * @param iterations the number of iterations. Users should aim for taking about .5
	 * seconds on their own system.
	 * @param hashWidth the size of the hash (in bits)
	 */
	public Pbkdf2PasswordEncoder(CharSequence secret, int iterations, int hashWidth) {
		this(secret, DEFAULT_SALT_LENGTH, iterations, hashWidth);
	}

	/**
	 * Constructs a standard password encoder with a secret value as well as salt length,
	 * iterations and hash width.
	 * @param secret the secret
	 * @param saltLength the salt length (in bytes)
	 * @param iterations the number of iterations. Users should aim for taking about .5
	 * seconds on their own system.
	 * @param hashWidth the size of the hash (in bits)
	 * @since 5.5
	 */
	public Pbkdf2PasswordEncoder(CharSequence secret, int saltLength, int iterations, int hashWidth) {
		this.secret = Utf8.encode(secret);
		this.saltGenerator = KeyGenerators.secureRandom(saltLength);
		this.iterations = iterations;
		this.hashWidth = hashWidth;
	}

	/**
	 * Sets the algorithm to use. See <a href=
	 * "https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SecretKeyFactory">SecretKeyFactory
	 * Algorithms</a>
	 * @param secretKeyFactoryAlgorithm the algorithm to use (i.e.
	 * {@code SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA1},
	 * {@code SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256},
	 * {@code SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512})
	 * @since 5.0
	 */
	public void setAlgorithm(SecretKeyFactoryAlgorithm secretKeyFactoryAlgorithm) {
		if (secretKeyFactoryAlgorithm == null) {
			throw new IllegalArgumentException("secretKeyFactoryAlgorithm cannot be null");
		}
		String algorithmName = secretKeyFactoryAlgorithm.name();
		try {
			SecretKeyFactory.getInstance(algorithmName);
			this.algorithm = algorithmName;
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalArgumentException("Invalid algorithm '" + algorithmName + "'.", ex);
		}
	}

	/**
	 * Sets if the resulting hash should be encoded as Base64. The default is false which
	 * means it will be encoded in Hex.
	 * @param encodeHashAsBase64 true if encode as Base64, false if should use Hex
	 * (default)
	 */
	public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
		this.encodeHashAsBase64 = encodeHashAsBase64;
	}

	@Override
	public String encode(CharSequence rawPassword) {
		byte[] salt = this.saltGenerator.generateKey();
		byte[] encoded = encode(rawPassword, salt);
		return encode(encoded);
	}

	private String encode(byte[] bytes) {
		if (this.encodeHashAsBase64) {
			return Base64.getEncoder().encodeToString(bytes);
		}
		return String.valueOf(Hex.encode(bytes));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		byte[] digested = decode(encodedPassword);
		byte[] salt = EncodingUtils.subArray(digested, 0, this.saltGenerator.getKeyLength());
		return MessageDigest.isEqual(digested, encode(rawPassword, salt));
	}

	private byte[] decode(String encodedBytes) {
		if (this.encodeHashAsBase64) {
			return Base64.getDecoder().decode(encodedBytes);
		}
		return Hex.decode(encodedBytes);
	}

	private byte[] encode(CharSequence rawPassword, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(rawPassword.toString().toCharArray(),
					EncodingUtils.concatenate(salt, this.secret), this.iterations, this.hashWidth);
			SecretKeyFactory skf = SecretKeyFactory.getInstance(this.algorithm);
			return EncodingUtils.concatenate(salt, skf.generateSecret(spec).getEncoded());
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not create hash", ex);
		}
	}

	/**
	 * The Algorithm used for creating the {@link SecretKeyFactory}
	 *
	 * @since 5.0
	 */
	public enum SecretKeyFactoryAlgorithm {

		PBKDF2WithHmacSHA1, PBKDF2WithHmacSHA256, PBKDF2WithHmacSHA512

	}

}
