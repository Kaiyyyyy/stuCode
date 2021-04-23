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

package org.springframework.security.oauth2.core;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.lang.Nullable;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

/**
 * Base class for OAuth 2.0 Token implementations.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see OAuth2Token
 * @see OAuth2AccessToken
 * @see OAuth2RefreshToken
 */
public abstract class AbstractOAuth2Token implements OAuth2Token, Serializable {

	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

	private final String tokenValue;

	private final Instant issuedAt;

	private final Instant expiresAt;

	/**
	 * Sub-class constructor.
	 * @param tokenValue the token value
	 */
	protected AbstractOAuth2Token(String tokenValue) {
		this(tokenValue, null, null);
	}

	/**
	 * Sub-class constructor.
	 * @param tokenValue the token value
	 * @param issuedAt the time at which the token was issued, may be {@code null}
	 * @param expiresAt the expiration time on or after which the token MUST NOT be
	 * accepted, may be {@code null}
	 */
	protected AbstractOAuth2Token(String tokenValue, @Nullable Instant issuedAt, @Nullable Instant expiresAt) {
		Assert.hasText(tokenValue, "tokenValue cannot be empty");
		if (issuedAt != null && expiresAt != null) {
			Assert.isTrue(expiresAt.isAfter(issuedAt), "expiresAt must be after issuedAt");
		}
		this.tokenValue = tokenValue;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	/**
	 * Returns the token value.
	 * @return the token value
	 */
	public String getTokenValue() {
		return this.tokenValue;
	}

	/**
	 * Returns the time at which the token was issued.
	 * @return the time the token was issued or {@code null}
	 */
	@Nullable
	public Instant getIssuedAt() {
		return this.issuedAt;
	}

	/**
	 * Returns the expiration time on or after which the token MUST NOT be accepted.
	 * @return the token expiration time or {@code null}
	 */
	@Nullable
	public Instant getExpiresAt() {
		return this.expiresAt;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		AbstractOAuth2Token other = (AbstractOAuth2Token) obj;
		if (!this.getTokenValue().equals(other.getTokenValue())) {
			return false;
		}
		if ((this.getIssuedAt() != null) ? !this.getIssuedAt().equals(other.getIssuedAt())
				: other.getIssuedAt() != null) {
			return false;
		}
		return (this.getExpiresAt() != null) ? this.getExpiresAt().equals(other.getExpiresAt())
				: other.getExpiresAt() == null;
	}

	@Override
	public int hashCode() {
		int result = this.getTokenValue().hashCode();
		result = 31 * result + ((this.getIssuedAt() != null) ? this.getIssuedAt().hashCode() : 0);
		result = 31 * result + ((this.getExpiresAt() != null) ? this.getExpiresAt().hashCode() : 0);
		return result;
	}

}
