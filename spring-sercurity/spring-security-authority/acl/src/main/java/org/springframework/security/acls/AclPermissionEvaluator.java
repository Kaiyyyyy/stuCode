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

package org.springframework.security.acls;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityGenerator;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;

/**
 * Used by Spring Security's expression-based access control implementation to evaluate
 * permissions for a particular object using the ACL module. Similar in behaviour to
 * {@link org.springframework.security.acls.AclEntryVoter AclEntryVoter}.
 *
 * @author Luke Taylor
 * @since 3.0
 */
public class AclPermissionEvaluator implements PermissionEvaluator {

	private final Log logger = LogFactory.getLog(getClass());

	private final AclService aclService;

	private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

	private ObjectIdentityGenerator objectIdentityGenerator = new ObjectIdentityRetrievalStrategyImpl();

	private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

	private PermissionFactory permissionFactory = new DefaultPermissionFactory();

	public AclPermissionEvaluator(AclService aclService) {
		this.aclService = aclService;
	}

	/**
	 * Determines whether the user has the given permission(s) on the domain object using
	 * the ACL configuration. If the domain object is null, returns false (this can always
	 * be overridden using a null check in the expression itself).
	 */
	@Override
	public boolean hasPermission(Authentication authentication, Object domainObject, Object permission) {
		if (domainObject == null) {
			return false;
		}
		ObjectIdentity objectIdentity = this.objectIdentityRetrievalStrategy.getObjectIdentity(domainObject);
		return checkPermission(authentication, objectIdentity, permission);
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
			Object permission) {
		ObjectIdentity objectIdentity = this.objectIdentityGenerator.createObjectIdentity(targetId, targetType);
		return checkPermission(authentication, objectIdentity, permission);
	}

	private boolean checkPermission(Authentication authentication, ObjectIdentity oid, Object permission) {
		// Obtain the SIDs applicable to the principal
		List<Sid> sids = this.sidRetrievalStrategy.getSids(authentication);
		List<Permission> requiredPermission = resolvePermission(permission);
		this.logger.debug(LogMessage.of(() -> "Checking permission '" + permission + "' for object '" + oid + "'"));
		try {
			// Lookup only ACLs for SIDs we're interested in
			Acl acl = this.aclService.readAclById(oid, sids);
			if (acl.isGranted(requiredPermission, sids, false)) {
				this.logger.debug("Access is granted");
				return true;
			}
			this.logger.debug("Returning false - ACLs returned, but insufficient permissions for this principal");
		}
		catch (NotFoundException nfe) {
			this.logger.debug("Returning false - no ACLs apply for this principal");
		}
		return false;
	}

	List<Permission> resolvePermission(Object permission) {
		if (permission instanceof Integer) {
			return Arrays.asList(this.permissionFactory.buildFromMask((Integer) permission));
		}
		if (permission instanceof Permission) {
			return Arrays.asList((Permission) permission);
		}
		if (permission instanceof Permission[]) {
			return Arrays.asList((Permission[]) permission);
		}
		if (permission instanceof String) {
			String permString = (String) permission;
			Permission p = buildPermission(permString);
			if (p != null) {
				return Arrays.asList(p);
			}
		}
		throw new IllegalArgumentException("Unsupported permission: " + permission);
	}

	private Permission buildPermission(String permString) {
		try {
			return this.permissionFactory.buildFromName(permString);
		}
		catch (IllegalArgumentException notfound) {
			return this.permissionFactory.buildFromName(permString.toUpperCase(Locale.ENGLISH));
		}
	}

	public void setObjectIdentityRetrievalStrategy(ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy) {
		this.objectIdentityRetrievalStrategy = objectIdentityRetrievalStrategy;
	}

	public void setObjectIdentityGenerator(ObjectIdentityGenerator objectIdentityGenerator) {
		this.objectIdentityGenerator = objectIdentityGenerator;
	}

	public void setSidRetrievalStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
		this.sidRetrievalStrategy = sidRetrievalStrategy;
	}

	public void setPermissionFactory(PermissionFactory permissionFactory) {
		this.permissionFactory = permissionFactory;
	}

}
