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

package org.springframework.security.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * 处理认证请求
 * Processes an {@link Authentication} request.
 *
 * @author Ben Alex
 */
public interface AuthenticationManager {

	/**
	 *
	 * 尝试对传递的身份验证对象进行身份验证，如果成功，则返回完全填充的身份验证对象（包括授予的权限）。
	 * AuthenticationManager必须遵守以下有关例外情况
	 * 	1、如果帐户被禁用并且AuthenticationManager可以测试此状态，则必须引发DisabledException
	 * 	2、如果帐户被锁定并且AuthenticationManager可以测试帐户锁定，则必须引发LockedException
	 * 	3、如果提供了不正确的凭据，则必须引发BadCredentialsException。尽管上述例外情况是可选的，但AuthenticationManager必须始终测试凭据
	 * 应测试异常，如果适用，则按上述顺序抛出（即，如果帐户被禁用或锁定，则立即拒绝身份验证请求，并且不执行凭据测试过程）。这可防止针对禁用或锁定的帐户测试凭据
	 * 返回包含凭据的完全认证的对象
	 * 如果认证失败抛出AuthenticationException
	 * Attempts to authenticate the passed {@link Authentication} object, returning a
	 * fully populated <code>Authentication</code> object (including granted authorities)
	 * if successful.
	 * <p>
	 * An <code>AuthenticationManager</code> must honour the following contract concerning
	 * exceptions:
	 * <ul>
	 * <li>A {@link DisabledException} must be thrown if an account is disabled and the
	 * <code>AuthenticationManager</code> can test for this state.</li>
	 * <li>A {@link LockedException} must be thrown if an account is locked and the
	 * <code>AuthenticationManager</code> can test for account locking.</li>
	 * <li>A {@link BadCredentialsException} must be thrown if incorrect credentials are
	 * presented. Whilst the above exceptions are optional, an
	 * <code>AuthenticationManager</code> must <B>always</B> test credentials.</li>
	 * </ul>
	 * Exceptions should be tested for and if applicable thrown in the order expressed
	 * above (i.e. if an account is disabled or locked, the authentication request is
	 * immediately rejected and the credentials testing process is not performed). This
	 * prevents credentials being tested against disabled or locked accounts.
	 * @param authentication the authentication request object
	 * @return a fully authenticated object including credentials
	 * @throws AuthenticationException if authentication fails
	 */
	Authentication authenticate(Authentication authentication) throws AuthenticationException;

}
