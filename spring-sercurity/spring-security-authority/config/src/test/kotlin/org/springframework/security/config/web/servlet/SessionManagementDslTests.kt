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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.test.SpringTestRule
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.session.SessionAuthenticationException
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Tests for [SessionManagementDsl]
 *
 * @author Eleftheria Stein
 */
class SessionManagementDslTests {
    @Rule
    @JvmField
    val spring = SpringTestRule()

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `session management when invalid session url then redirected to url`() {
        this.spring.register(InvalidSessionUrlConfig::class.java).autowire()

        this.mockMvc.perform(get("/")
                .with { request ->
                    request.isRequestedSessionIdValid = false
                    request.requestedSessionId = "id"
                    request
                })
                .andExpect(status().isFound)
                .andExpect(redirectedUrl("/invalid"))
    }

    @EnableWebSecurity
    open class InvalidSessionUrlConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                sessionManagement {
                    invalidSessionUrl = "/invalid"
                }
            }
        }
    }

    @Test
    fun `session management when invalid session strategy then strategy used`() {
        this.spring.register(InvalidSessionStrategyConfig::class.java).autowire()

        this.mockMvc.perform(get("/")
                .with { request ->
                    request.isRequestedSessionIdValid = false
                    request.requestedSessionId = "id"
                    request
                })
                .andExpect(status().isFound)
                .andExpect(redirectedUrl("/invalid"))
    }

    @EnableWebSecurity
    open class InvalidSessionStrategyConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                sessionManagement {
                    invalidSessionStrategy = SimpleRedirectInvalidSessionStrategy("/invalid")
                }
            }
        }
    }

    @Test
    fun `session management when session authentication error url then redirected to url`() {
        this.spring.register(SessionAuthenticationErrorUrlConfig::class.java).autowire()
        val session = mock(MockHttpSession::class.java)
        `when`(session.changeSessionId()).thenThrow(SessionAuthenticationException::class.java)

        this.mockMvc.perform(get("/")
                .with(authentication(mock(Authentication::class.java)))
                .session(session))
                .andExpect(status().isFound)
                .andExpect(redirectedUrl("/session-auth-error"))
    }

    @EnableWebSecurity
    open class SessionAuthenticationErrorUrlConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                authorizeRequests {
                    authorize(anyRequest, authenticated)
                }
                sessionManagement {
                    sessionAuthenticationErrorUrl = "/session-auth-error"
                }
            }
        }
    }

    @Test
    fun `session management when session authentication failure handler then handler used`() {
        this.spring.register(SessionAuthenticationFailureHandlerConfig::class.java).autowire()
        val session = mock(MockHttpSession::class.java)
        `when`(session.changeSessionId()).thenThrow(SessionAuthenticationException::class.java)

        this.mockMvc.perform(get("/")
                .with(authentication(mock(Authentication::class.java)))
                .session(session))
                .andExpect(status().isFound)
                .andExpect(redirectedUrl("/session-auth-error"))
    }

    @EnableWebSecurity
    open class SessionAuthenticationFailureHandlerConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                authorizeRequests {
                    authorize(anyRequest, authenticated)
                }
                sessionManagement {
                    sessionAuthenticationFailureHandler = SimpleUrlAuthenticationFailureHandler("/session-auth-error")
                }
            }
        }
    }

    @Test
    fun `session management when stateless policy then does not store session`() {
        this.spring.register(StatelessSessionManagementConfig::class.java).autowire()

        val result = this.mockMvc.perform(get("/"))
                .andReturn()

        assertThat(result.request.getSession(false)).isNull()
    }

    @EnableWebSecurity
    open class StatelessSessionManagementConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                authorizeRequests {
                    authorize(anyRequest, authenticated)
                }
                sessionManagement {
                    sessionCreationPolicy = SessionCreationPolicy.STATELESS
                }
            }
        }
    }

    @Test
    fun `session management when session authentication strategy then strategy used`() {
        this.spring.register(SessionAuthenticationStrategyConfig::class.java).autowire()

        this.mockMvc.perform(get("/")
                .with(authentication(mock(Authentication::class.java)))
                .session(mock(MockHttpSession::class.java)))

        verify(this.spring.getContext().getBean(SessionAuthenticationStrategy::class.java))
                .onAuthentication(any(Authentication::class.java),
                        any(HttpServletRequest::class.java), any(HttpServletResponse::class.java))
    }

    @EnableWebSecurity
    open class SessionAuthenticationStrategyConfig : WebSecurityConfigurerAdapter() {
        var mockSessionAuthenticationStrategy: SessionAuthenticationStrategy = mock(SessionAuthenticationStrategy::class.java)

        override fun configure(http: HttpSecurity) {
            http {
                authorizeRequests {
                    authorize(anyRequest, authenticated)
                }
                sessionManagement {
                    sessionAuthenticationStrategy = mockSessionAuthenticationStrategy
                }
            }
        }

        @Bean
        open fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
            return this.mockSessionAuthenticationStrategy
        }
    }
}
