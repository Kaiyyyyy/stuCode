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

package org.springframework.security.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Spring Security extension to Spring's {@link WebApplicationContextUtils}.
 *
 * @author Rob Winch
 */
public abstract class SecurityWebApplicationContextUtils extends WebApplicationContextUtils {

	/**
	 * Find a unique {@code WebApplicationContext} for this web app: either the root web
	 * app context (preferred) or a unique {@code WebApplicationContext} among the
	 * registered {@code ServletContext} attributes (typically coming from a single
	 * {@code DispatcherServlet} in the current web application).
	 * <p>
	 * Note that {@code DispatcherServlet}'s exposure of its context can be controlled
	 * through its {@code publishContext} property, which is {@code true} by default but
	 * can be selectively switched to only publish a single context despite multiple
	 * {@code DispatcherServlet} registrations in the web app.
	 * @param servletContext ServletContext to find the web application context for
	 * @return the desired WebApplicationContext for this web app
	 * @throws IllegalStateException if no WebApplicationContext can be found
	 * @see #getWebApplicationContext(ServletContext)
	 * @see ServletContext#getAttributeNames()
	 */
	public static WebApplicationContext findRequiredWebApplicationContext(ServletContext servletContext) {
		WebApplicationContext webApplicationContext = findWebApplicationContext(servletContext);
		Assert.state(webApplicationContext != null,
				"No WebApplicationContext found: no ContextLoaderListener registered?");
		return webApplicationContext;
	}

}
