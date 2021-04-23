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

package org.springframework.security.cas.authentication;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests {@link EhCacheBasedTicketCache}.
 *
 * @author Ben Alex
 */
public class EhCacheBasedTicketCacheTests extends AbstractStatelessTicketCacheTests {

	private static CacheManager cacheManager;

	@BeforeClass
	public static void initCacheManaer() {
		cacheManager = CacheManager.create();
		cacheManager.addCache(new Cache("castickets", 500, false, false, 30, 30));
	}

	@AfterClass
	public static void shutdownCacheManager() {
		cacheManager.removalAll();
		cacheManager.shutdown();
	}

	@Test
	public void testCacheOperation() throws Exception {
		EhCacheBasedTicketCache cache = new EhCacheBasedTicketCache();
		cache.setCache(cacheManager.getCache("castickets"));
		cache.afterPropertiesSet();
		final CasAuthenticationToken token = getToken();
		// Check it gets stored in the cache
		cache.putTicketInCache(token);
		assertThat(cache.getByTicketId("ST-0-ER94xMJmn6pha35CQRoZ")).isEqualTo(token);
		// Check it gets removed from the cache
		cache.removeTicketFromCache(getToken());
		assertThat(cache.getByTicketId("ST-0-ER94xMJmn6pha35CQRoZ")).isNull();
		// Check it doesn't return values for null or unknown service tickets
		assertThat(cache.getByTicketId(null)).isNull();
		assertThat(cache.getByTicketId("UNKNOWN_SERVICE_TICKET")).isNull();
	}

	@Test
	public void testStartupDetectsMissingCache() throws Exception {
		EhCacheBasedTicketCache cache = new EhCacheBasedTicketCache();
		assertThatIllegalArgumentException().isThrownBy(cache::afterPropertiesSet);
		Ehcache myCache = cacheManager.getCache("castickets");
		cache.setCache(myCache);
		assertThat(cache.getCache()).isEqualTo(myCache);
	}

}
