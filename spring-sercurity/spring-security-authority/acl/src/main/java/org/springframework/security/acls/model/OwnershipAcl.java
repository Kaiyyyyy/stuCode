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

package org.springframework.security.acls.model;

/**
 * A mutable ACL that provides ownership capabilities.
 *
 * <p>
 * Generally the owner of an ACL is able to call any ACL mutator method, as well as assign
 * a new owner.
 *
 * @author Ben Alex
 */
public interface OwnershipAcl extends MutableAcl {

	@Override
	void setOwner(Sid newOwner);

}
