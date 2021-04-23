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

package org.springframework.security.ldap.userdetails;

import java.util.Collection;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * @author Luke Taylor
 */
public class PersonContextMapper implements UserDetailsContextMapper {

	@Override
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
			Collection<? extends GrantedAuthority> authorities) {
		Person.Essence p = new Person.Essence(ctx);
		p.setUsername(username);
		p.setAuthorities(authorities);
		return p.createUserDetails();
	}

	@Override
	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		Assert.isInstanceOf(Person.class, user, "UserDetails must be a Person instance");
		Person p = (Person) user;
		p.populateContext(ctx);
	}

}
