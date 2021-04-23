/*
 * Copyright 2009-2016 the original author or authors.
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

package org.springframework.security.remoting.dns;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Mike Wiesner
 * @since 3.0
 */
public class JndiDnsResolverTests {

	private JndiDnsResolver dnsResolver;

	private InitialContextFactory contextFactory;

	private DirContext context;

	@Before
	public void setup() {
		this.contextFactory = mock(InitialContextFactory.class);
		this.context = mock(DirContext.class);
		this.dnsResolver = new JndiDnsResolver();
		this.dnsResolver.setCtxFactory(this.contextFactory);
		given(this.contextFactory.getCtx()).willReturn(this.context);
	}

	@Test
	public void testResolveIpAddress() throws Exception {
		Attributes records = new BasicAttributes("A", "63.246.7.80");
		given(this.context.getAttributes("www.springsource.com", new String[] { "A" })).willReturn(records);
		String ipAddress = this.dnsResolver.resolveIpAddress("www.springsource.com");
		assertThat(ipAddress).isEqualTo("63.246.7.80");
	}

	@Test
	public void testResolveIpAddressNotExisting() throws Exception {
		given(this.context.getAttributes(any(String.class), any(String[].class)))
				.willThrow(new NameNotFoundException("not found"));
		assertThatExceptionOfType(DnsEntryNotFoundException.class)
				.isThrownBy(() -> this.dnsResolver.resolveIpAddress("notexisting.ansdansdugiuzgguzgioansdiandwq.foo"));
	}

	@Test
	public void testResolveServiceEntry() throws Exception {
		BasicAttributes records = createSrvRecords();
		given(this.context.getAttributes("_ldap._tcp.springsource.com", new String[] { "SRV" })).willReturn(records);
		String hostname = this.dnsResolver.resolveServiceEntry("ldap", "springsource.com");
		assertThat(hostname).isEqualTo("kdc.springsource.com");
	}

	@Test
	public void testResolveServiceEntryNotExisting() throws Exception {
		given(this.context.getAttributes(any(String.class), any(String[].class)))
				.willThrow(new NameNotFoundException("not found"));
		assertThatExceptionOfType(DnsEntryNotFoundException.class)
				.isThrownBy(() -> this.dnsResolver.resolveServiceEntry("wrong", "secpod.de"));
	}

	@Test
	public void testResolveServiceIpAddress() throws Exception {
		BasicAttributes srvRecords = createSrvRecords();
		BasicAttributes aRecords = new BasicAttributes("A", "63.246.7.80");
		given(this.context.getAttributes("_ldap._tcp.springsource.com", new String[] { "SRV" })).willReturn(srvRecords);
		given(this.context.getAttributes("kdc.springsource.com", new String[] { "A" })).willReturn(aRecords);
		String ipAddress = this.dnsResolver.resolveServiceIpAddress("ldap", "springsource.com");
		assertThat(ipAddress).isEqualTo("63.246.7.80");
	}

	@Test
	public void testUnknowError() throws Exception {
		given(this.context.getAttributes(any(String.class), any(String[].class)))
				.willThrow(new NamingException("error"));
		assertThatExceptionOfType(DnsLookupException.class).isThrownBy(() -> this.dnsResolver.resolveIpAddress(""));
	}

	private BasicAttributes createSrvRecords() {
		BasicAttributes records = new BasicAttributes();
		BasicAttribute record = new BasicAttribute("SRV");
		// the structure of the service records is:
		// priority weight port hostname
		// for more information: https://en.wikipedia.org/wiki/SRV_record
		record.add("20 80 389 kdc3.springsource.com.");
		record.add("10 70 389 kdc.springsource.com.");
		record.add("20 20 389 kdc4.springsource.com.");
		record.add("10 30 389 kdc2.springsource.com");
		records.put(record);
		return records;
	}

}
