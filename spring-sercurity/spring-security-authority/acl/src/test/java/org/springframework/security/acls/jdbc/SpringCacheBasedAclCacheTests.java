/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.security.acls.jdbc;

import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.FieldUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests {@link org.springframework.security.acls.domain.SpringCacheBasedAclCache}
 *
 * @author Marten Deinum
 */
public class SpringCacheBasedAclCacheTests {

	private static final String TARGET_CLASS = "org.springframework.security.acls.TargetObject";

	private static CacheManager cacheManager;

	@BeforeClass
	public static void initCacheManaer() {
		cacheManager = new ConcurrentMapCacheManager();
		// Use disk caching immediately (to test for serialization issue reported in
		// SEC-527)
		cacheManager.getCache("springcasebasedacltests");
	}

	@After
	public void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private Cache getCache() {
		Cache cache = cacheManager.getCache("springcasebasedacltests");
		cache.clear();
		return cache;
	}

	@Test
	public void constructorRejectsNullParameters() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SpringCacheBasedAclCache(null, null, null));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void cacheOperationsAclWithoutParent() {
		Cache cache = getCache();
		Map realCache = (Map) cache.getNativeCache();
		ObjectIdentity identity = new ObjectIdentityImpl(TARGET_CLASS, 100L);
		AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
				new SimpleGrantedAuthority("ROLE_OWNERSHIP"), new SimpleGrantedAuthority("ROLE_AUDITING"),
				new SimpleGrantedAuthority("ROLE_GENERAL"));
		AuditLogger auditLogger = new ConsoleAuditLogger();
		PermissionGrantingStrategy permissionGrantingStrategy = new DefaultPermissionGrantingStrategy(auditLogger);
		SpringCacheBasedAclCache myCache = new SpringCacheBasedAclCache(cache, permissionGrantingStrategy,
				aclAuthorizationStrategy);
		MutableAcl acl = new AclImpl(identity, 1L, aclAuthorizationStrategy, auditLogger);
		assertThat(realCache).isEmpty();
		myCache.putInCache(acl);
		// Check we can get from cache the same objects we put in
		assertThat(acl).isEqualTo(myCache.getFromCache(1L));
		assertThat(acl).isEqualTo(myCache.getFromCache(identity));
		// Put another object in cache
		ObjectIdentity identity2 = new ObjectIdentityImpl(TARGET_CLASS, 101L);
		MutableAcl acl2 = new AclImpl(identity2, 2L, aclAuthorizationStrategy, new ConsoleAuditLogger());
		myCache.putInCache(acl2);
		// Try to evict an entry that doesn't exist
		myCache.evictFromCache(3L);
		myCache.evictFromCache(new ObjectIdentityImpl(TARGET_CLASS, 102L));
		assertThat(realCache).hasSize(4);
		myCache.evictFromCache(1L);
		assertThat(realCache).hasSize(2);
		// Check the second object inserted
		assertThat(acl2).isEqualTo(myCache.getFromCache(2L));
		assertThat(acl2).isEqualTo(myCache.getFromCache(identity2));
		myCache.evictFromCache(identity2);
		assertThat(realCache).isEmpty();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void cacheOperationsAclWithParent() throws Exception {
		Cache cache = getCache();
		Map realCache = (Map) cache.getNativeCache();
		Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_GENERAL");
		auth.setAuthenticated(true);
		SecurityContextHolder.getContext().setAuthentication(auth);
		ObjectIdentity identity = new ObjectIdentityImpl(TARGET_CLASS, 1L);
		ObjectIdentity identityParent = new ObjectIdentityImpl(TARGET_CLASS, 2L);
		AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
				new SimpleGrantedAuthority("ROLE_OWNERSHIP"), new SimpleGrantedAuthority("ROLE_AUDITING"),
				new SimpleGrantedAuthority("ROLE_GENERAL"));
		AuditLogger auditLogger = new ConsoleAuditLogger();
		PermissionGrantingStrategy permissionGrantingStrategy = new DefaultPermissionGrantingStrategy(auditLogger);
		SpringCacheBasedAclCache myCache = new SpringCacheBasedAclCache(cache, permissionGrantingStrategy,
				aclAuthorizationStrategy);
		MutableAcl acl = new AclImpl(identity, 1L, aclAuthorizationStrategy, auditLogger);
		MutableAcl parentAcl = new AclImpl(identityParent, 2L, aclAuthorizationStrategy, auditLogger);
		acl.setParent(parentAcl);
		assertThat(realCache).isEmpty();
		myCache.putInCache(acl);
		assertThat(4).isEqualTo(realCache.size());
		// Check we can get from cache the same objects we put in
		AclImpl aclFromCache = (AclImpl) myCache.getFromCache(1L);
		assertThat(aclFromCache).isEqualTo(acl);
		// SEC-951 check transient fields are set on parent
		assertThat(FieldUtils.getFieldValue(aclFromCache.getParentAcl(), "aclAuthorizationStrategy")).isNotNull();
		assertThat(FieldUtils.getFieldValue(aclFromCache.getParentAcl(), "permissionGrantingStrategy")).isNotNull();
		assertThat(myCache.getFromCache(identity)).isEqualTo(acl);
		assertThat(FieldUtils.getFieldValue(aclFromCache, "aclAuthorizationStrategy")).isNotNull();
		AclImpl parentAclFromCache = (AclImpl) myCache.getFromCache(2L);
		assertThat(parentAclFromCache).isEqualTo(parentAcl);
		assertThat(FieldUtils.getFieldValue(parentAclFromCache, "aclAuthorizationStrategy")).isNotNull();
		assertThat(myCache.getFromCache(identityParent)).isEqualTo(parentAcl);
	}

}
