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

package org.springframework.security.acls.domain;

import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AuditableAccessControlEntry;
import org.springframework.util.Assert;

/**
 * A basic implementation of {@link AuditLogger}.
 *
 * @author Ben Alex
 */
public class ConsoleAuditLogger implements AuditLogger {

	@Override
	public void logIfNeeded(boolean granted, AccessControlEntry ace) {
		Assert.notNull(ace, "AccessControlEntry required");
		if (ace instanceof AuditableAccessControlEntry) {
			AuditableAccessControlEntry auditableAce = (AuditableAccessControlEntry) ace;
			if (granted && auditableAce.isAuditSuccess()) {
				System.out.println("GRANTED due to ACE: " + ace);
			}
			else if (!granted && auditableAce.isAuditFailure()) {
				System.out.println("DENIED due to ACE: " + ace);
			}
		}
	}

}
