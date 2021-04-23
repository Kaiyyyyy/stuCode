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

package org.springframework.security.config.web.servlet

import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.test.SpringTestRule
import org.springframework.security.saml2.credentials.Saml2X509Credential
import org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialType.VERIFICATION
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.security.cert.Certificate
import java.security.cert.CertificateFactory

/**
 * Tests for [Saml2Dsl]
 *
 * @author Eleftheria Stein
 */
class Saml2DslTests {
    @Rule
    @JvmField
    val spring = SpringTestRule()

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `saml2Login when no relying party registration repository then exception`() {
        Assertions.assertThatThrownBy { this.spring.register(Saml2LoginNoRelyingPArtyRegistrationRepoConfig::class.java).autowire() }
                .isInstanceOf(BeanCreationException::class.java)
                .hasMessageContaining("relyingPartyRegistrationRepository cannot be null")

    }

    @EnableWebSecurity
    open class Saml2LoginNoRelyingPArtyRegistrationRepoConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                saml2Login { }
            }
        }
    }

    @Test
    fun `login page when saml2Configured then default login page created`() {
        this.spring.register(Saml2LoginConfig::class.java).autowire()

        this.mockMvc.get("/login")
                .andExpect {
                    status { isOk() }
                }
    }

    @EnableWebSecurity
    open class Saml2LoginConfig : WebSecurityConfigurerAdapter() {

        override fun configure(http: HttpSecurity) {
            http {
                saml2Login {
                    relyingPartyRegistrationRepository =
                            InMemoryRelyingPartyRegistrationRepository(
                                    RelyingPartyRegistration.withRegistrationId("samlId")
                                            .assertionConsumerServiceUrlTemplate("{baseUrl}" + Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI)
                                            .credentials { c -> c.add(Saml2X509Credential(loadCert("rod.cer"), VERIFICATION)) }
                                            .providerDetails { c -> c.webSsoUrl("ssoUrl") }
                                            .providerDetails { c -> c.entityId("entityId") }
                                            .build()
                            )
                }
            }
        }

        private fun <T : Certificate> loadCert(location: String): T {
            ClassPathResource(location).inputStream.use { inputStream ->
                val certFactory = CertificateFactory.getInstance("X.509")
                return certFactory.generateCertificate(inputStream) as T
            }
        }
    }
}
