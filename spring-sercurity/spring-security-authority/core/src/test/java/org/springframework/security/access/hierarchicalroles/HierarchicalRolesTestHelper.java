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

package org.springframework.security.access.hierarchicalroles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import org.springframework.security.core.GrantedAuthority;

/**
 * Test helper class for the hierarchical roles tests.
 *
 * @author Michael Mayr
 */
public abstract class HierarchicalRolesTestHelper {

	public static boolean containTheSameGrantedAuthorities(Collection<? extends GrantedAuthority> authorities1,
			Collection<? extends GrantedAuthority> authorities2) {
		if (authorities1 == null && authorities2 == null) {
			return true;
		}
		if (authorities1 == null || authorities2 == null) {
			return false;
		}
		return CollectionUtils.isEqualCollection(authorities1, authorities2);
	}

	public static boolean containTheSameGrantedAuthoritiesCompareByAuthorityString(
			Collection<? extends GrantedAuthority> authorities1, Collection<? extends GrantedAuthority> authorities2) {
		if (authorities1 == null && authorities2 == null) {
			return true;
		}
		if (authorities1 == null || authorities2 == null) {
			return false;
		}
		return CollectionUtils.isEqualCollection(toCollectionOfAuthorityStrings(authorities1),
				toCollectionOfAuthorityStrings(authorities2));
	}

	public static List<String> toCollectionOfAuthorityStrings(Collection<? extends GrantedAuthority> authorities) {
		if (authorities == null) {
			return null;
		}
		List<String> result = new ArrayList<>(authorities.size());
		for (GrantedAuthority authority : authorities) {
			result.add(authority.getAuthority());
		}
		return result;
	}

	public static List<GrantedAuthority> createAuthorityList(final String... roles) {
		List<GrantedAuthority> authorities = new ArrayList<>(roles.length);
		for (final String role : roles) {
			// Use non SimpleGrantedAuthority (SEC-863)
			authorities.add((GrantedAuthority) () -> role);
		}
		return authorities;
	}

}
