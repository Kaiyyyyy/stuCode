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

package org.springframework.security.acls.domain;

import java.io.Serializable;

import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AuditableAccessControlEntry;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;

/**
 * An immutable default implementation of <code>AccessControlEntry</code>.
 *
 * @author Ben Alex
 */
public class AccessControlEntryImpl implements AccessControlEntry, AuditableAccessControlEntry {

	private final Acl acl;

	private Permission permission;

	private final Serializable id;

	private final Sid sid;

	private boolean auditFailure = false;

	private boolean auditSuccess = false;

	private final boolean granting;

	public AccessControlEntryImpl(Serializable id, Acl acl, Sid sid, Permission permission, boolean granting,
			boolean auditSuccess, boolean auditFailure) {
		Assert.notNull(acl, "Acl required");
		Assert.notNull(sid, "Sid required");
		Assert.notNull(permission, "Permission required");
		this.id = id;
		this.acl = acl; // can be null
		this.sid = sid;
		this.permission = permission;
		this.granting = granting;
		this.auditSuccess = auditSuccess;
		this.auditFailure = auditFailure;
	}

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof AccessControlEntryImpl)) {
			return false;
		}
		AccessControlEntryImpl other = (AccessControlEntryImpl) arg0;
		if (this.acl == null) {
			if (other.getAcl() != null) {
				return false;
			}
			// Both this.acl and rhs.acl are null and thus equal
		}
		else {
			// this.acl is non-null
			if (other.getAcl() == null) {
				return false;
			}

			// Both this.acl and rhs.acl are non-null, so do a comparison
			if (this.acl.getObjectIdentity() == null) {
				if (other.acl.getObjectIdentity() != null) {
					return false;
				}
				// Both this.acl and rhs.acl are null and thus equal
			}
			else {
				// Both this.acl.objectIdentity and rhs.acl.objectIdentity are non-null
				if (!this.acl.getObjectIdentity().equals(other.getAcl().getObjectIdentity())) {
					return false;
				}
			}
		}
		if (this.id == null) {
			if (other.id != null) {
				return false;
			}
			// Both this.id and rhs.id are null and thus equal
		}
		else {
			// this.id is non-null
			if (other.id == null) {
				return false;
			}
			// Both this.id and rhs.id are non-null
			if (!this.id.equals(other.id)) {
				return false;
			}
		}
		if ((this.auditFailure != other.isAuditFailure()) || (this.auditSuccess != other.isAuditSuccess())
				|| (this.granting != other.isGranting()) || !this.permission.equals(other.getPermission())
				|| !this.sid.equals(other.getSid())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = this.permission.hashCode();
		result = 31 * result + ((this.id != null) ? this.id.hashCode() : 0);
		result = 31 * result + (this.sid.hashCode());
		result = 31 * result + (this.auditFailure ? 1 : 0);
		result = 31 * result + (this.auditSuccess ? 1 : 0);
		result = 31 * result + (this.granting ? 1 : 0);
		return result;
	}

	@Override
	public Acl getAcl() {
		return this.acl;
	}

	@Override
	public Serializable getId() {
		return this.id;
	}

	@Override
	public Permission getPermission() {
		return this.permission;
	}

	@Override
	public Sid getSid() {
		return this.sid;
	}

	@Override
	public boolean isAuditFailure() {
		return this.auditFailure;
	}

	@Override
	public boolean isAuditSuccess() {
		return this.auditSuccess;
	}

	@Override
	public boolean isGranting() {
		return this.granting;
	}

	void setAuditFailure(boolean auditFailure) {
		this.auditFailure = auditFailure;
	}

	void setAuditSuccess(boolean auditSuccess) {
		this.auditSuccess = auditSuccess;
	}

	void setPermission(Permission permission) {
		Assert.notNull(permission, "Permission required");
		this.permission = permission;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AccessControlEntryImpl[");
		sb.append("id: ").append(this.id).append("; ");
		sb.append("granting: ").append(this.granting).append("; ");
		sb.append("sid: ").append(this.sid).append("; ");
		sb.append("permission: ").append(this.permission).append("; ");
		sb.append("auditSuccess: ").append(this.auditSuccess).append("; ");
		sb.append("auditFailure: ").append(this.auditFailure);
		sb.append("]");
		return sb.toString();
	}

}
