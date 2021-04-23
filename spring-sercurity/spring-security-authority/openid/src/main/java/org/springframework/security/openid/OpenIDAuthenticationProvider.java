/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.openid;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

/**
 * Finalises the OpenID authentication by obtaining local authorities for the
 * authenticated user.
 * <p>
 * The authorities are obtained by calling the configured {@code UserDetailsService}. The
 * {@code UserDetails} it returns must, at minimum, contain the username and
 * {@code GrantedAuthority} objects applicable to the authenticated user. Note that by
 * default, Spring Security ignores the password and enabled/disabled status of the
 * {@code UserDetails} because this is authentication-related and should have been
 * enforced by another provider server.
 * <p>
 * The {@code UserDetails} returned by implementations is stored in the generated
 * {@code Authentication} token, so additional properties such as email addresses,
 * telephone numbers etc can easily be stored.
 *
 * @author Robin Bramley, Opsera Ltd.
 * @author Luke Taylor
 * @deprecated The OpenID 1.0 and 2.0 protocols have been deprecated and users are
 * <a href="https://openid.net/specs/openid-connect-migration-1_0.html">encouraged to
 * migrate</a> to <a href="https://openid.net/connect/">OpenID Connect</a>, which is
 * supported by <code>spring-security-oauth2</code>.
 */
@Deprecated
public class OpenIDAuthenticationProvider implements AuthenticationProvider, InitializingBean {

	private AuthenticationUserDetailsService<OpenIDAuthenticationToken> userDetailsService;

	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.userDetailsService, "The userDetailsService must be set");
	}

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		if (!supports(authentication.getClass())) {
			return null;
		}
		if (!(authentication instanceof OpenIDAuthenticationToken)) {
			return null;
		}
		OpenIDAuthenticationToken response = (OpenIDAuthenticationToken) authentication;
		OpenIDAuthenticationStatus status = response.getStatus();
		// handle the various possibilities
		if (status == OpenIDAuthenticationStatus.SUCCESS) {
			// Lookup user details
			UserDetails userDetails = this.userDetailsService.loadUserDetails(response);
			return createSuccessfulAuthentication(userDetails, response);
		}
		if (status == OpenIDAuthenticationStatus.CANCELLED) {
			throw new AuthenticationCancelledException("Log in cancelled");
		}
		if (status == OpenIDAuthenticationStatus.ERROR) {
			throw new AuthenticationServiceException("Error message from server: " + response.getMessage());
		}
		if (status == OpenIDAuthenticationStatus.FAILURE) {
			throw new BadCredentialsException("Log in failed - identity could not be verified");
		}
		if (status == OpenIDAuthenticationStatus.SETUP_NEEDED) {
			throw new AuthenticationServiceException("The server responded setup was needed, which shouldn't happen");
		}
		throw new AuthenticationServiceException("Unrecognized return value " + status.toString());
	}

	/**
	 * Handles the creation of the final <tt>Authentication</tt> object which will be
	 * returned by the provider.
	 * <p>
	 * The default implementation just creates a new OpenIDAuthenticationToken from the
	 * original, but with the UserDetails as the principal and including the authorities
	 * loaded by the UserDetailsService.
	 * @param userDetails the loaded UserDetails object
	 * @param auth the token passed to the authenticate method, containing
	 * @return the token which will represent the authenticated user.
	 */
	protected Authentication createSuccessfulAuthentication(UserDetails userDetails, OpenIDAuthenticationToken auth) {
		return new OpenIDAuthenticationToken(userDetails,
				this.authoritiesMapper.mapAuthorities(userDetails.getAuthorities()), auth.getIdentityUrl(),
				auth.getAttributes());
	}

	/**
	 * Used to load the {@code UserDetails} for the authenticated OpenID user.
	 */
	public void setUserDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = new UserDetailsByNameServiceWrapper<>(userDetailsService);
	}

	/**
	 * Used to load the {@code UserDetails} for the authenticated OpenID user.
	 */
	public void setAuthenticationUserDetailsService(
			AuthenticationUserDetailsService<OpenIDAuthenticationToken> userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OpenIDAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		this.authoritiesMapper = authoritiesMapper;
	}

}
