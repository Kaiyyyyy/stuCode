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

package org.springframework.security.config.web.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.config.test.SpringTestRule
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.test.web.reactive.server.FluxExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.BodyInserters

/**
 * Tests for [ServerFormLoginDsl]
 *
 * @author Eleftheria Stein
 */
class ServerFormLoginDslTests {
    @Rule
    @JvmField
    val spring = SpringTestRule()

    private lateinit var client: WebTestClient

    @Autowired
    fun setup(context: ApplicationContext) {
        this.client = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .build()
    }

    @Test
    fun `request when form login enabled then redirects to default login page`() {
        this.spring.register(DefaultFormLoginConfig::class.java, UserDetailsConfig::class.java).autowire()

        val result: FluxExchangeResult<String> = this.client.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection
                .returnResult(String::class.java)

        result.assertWithDiagnostics {
            assertThat(result.responseHeaders.location).hasPath("/login")
        }
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class DefaultFormLoginConfig {
        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                formLogin { }
            }
        }
    }

    @Test
    fun `request when custom login page then redirects to custom login page`() {
        this.spring.register(CustomLoginPageConfig::class.java, UserDetailsConfig::class.java).autowire()

        val result: FluxExchangeResult<String> = this.client.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection
                .returnResult(String::class.java)

        result.assertWithDiagnostics {
            assertThat(result.responseHeaders.location).hasPath("/log-in")
        }
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class CustomLoginPageConfig {
        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                formLogin {
                    loginPage = "/log-in"
                }
            }
        }
    }

    @Test
    fun `form login when custom authentication manager then manager used`() {
        this.spring.register(CustomAuthenticationManagerConfig::class.java).autowire()
        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data.add("username", "user")
        data.add("password", "password")

        this.client
                .mutateWith(csrf())
                .post()
                .uri("/login")
                .body(BodyInserters.fromFormData(data))
                .exchange()

        verify<ReactiveAuthenticationManager>(CustomAuthenticationManagerConfig.AUTHENTICATION_MANAGER)
                .authenticate(any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class CustomAuthenticationManagerConfig {
        companion object {
            var AUTHENTICATION_MANAGER: ReactiveAuthenticationManager = Mockito.mock(ReactiveAuthenticationManager::class.java)
        }

        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                formLogin {
                    authenticationManager = AUTHENTICATION_MANAGER
                }
            }
        }
    }

    @Test
    fun `form login when custom authentication entry point then entry point used`() {
        this.spring.register(CustomConfig::class.java, UserDetailsConfig::class.java).autowire()

        val result = this.client.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection
                .returnResult(String::class.java)

        result.assertWithDiagnostics {
            assertThat(result.responseHeaders.location).hasPath("/entry")
        }
    }

    @Test
    fun `form login when custom requires authentication matcher then matching request logs in`() {
        this.spring.register(CustomConfig::class.java, UserDetailsConfig::class.java).autowire()
        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data.add("username", "user")
        data.add("password", "password")

        val result = this.client
                .mutateWith(csrf())
                .post()
                .uri("/log-in")
                .body(BodyInserters.fromFormData(data))
                .exchange()
                .expectStatus().is3xxRedirection
                .returnResult(String::class.java)

        result.assertWithDiagnostics {
            assertThat(result.responseHeaders.location).hasPath("/")
        }
    }

    @Test
    fun `invalid login when custom failure handler then failure handler used`() {
        this.spring.register(CustomConfig::class.java, UserDetailsConfig::class.java).autowire()

        val result = this.client
                .mutateWith(csrf())
                .post()
                .uri("/log-in")
                .exchange()
                .expectStatus().is3xxRedirection
                .returnResult(String::class.java)

        result.assertWithDiagnostics {
            assertThat(result.responseHeaders.location).hasPath("/log-in-error")
        }
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class CustomConfig {
        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                formLogin {
                    authenticationEntryPoint = RedirectServerAuthenticationEntryPoint("/entry")
                    requiresAuthenticationMatcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/log-in")
                    authenticationFailureHandler = RedirectServerAuthenticationFailureHandler("/log-in-error")
                }
            }
        }
    }

    @Test
    fun `login when custom success handler then success handler used`() {
        this.spring.register(CustomSuccessHandlerConfig::class.java, UserDetailsConfig::class.java).autowire()
        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data.add("username", "user")
        data.add("password", "password")

        val result = this.client
                .mutateWith(csrf())
                .post()
                .uri("/login")
                .body(BodyInserters.fromFormData(data))
                .exchange()
                .expectStatus().is3xxRedirection
                .returnResult(String::class.java)

        result.assertWithDiagnostics {
            assertThat(result.responseHeaders.location).hasPath("/success")
        }
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class CustomSuccessHandlerConfig {
        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                formLogin {
                    authenticationSuccessHandler = RedirectServerAuthenticationSuccessHandler("/success")
                }
            }
        }
    }

    @Test
    fun `form login when custom security context repository then repository used`() {
        this.spring.register(CustomSecurityContextRepositoryConfig::class.java, UserDetailsConfig::class.java).autowire()
        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data.add("username", "user")
        data.add("password", "password")

        this.client
                .mutateWith(csrf())
                .post()
                .uri("/login")
                .body(BodyInserters.fromFormData(data))
                .exchange()

        verify<ServerSecurityContextRepository>(CustomSecurityContextRepositoryConfig.SECURITY_CONTEXT_REPOSITORY)
                .save(Mockito.any(), Mockito.any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class CustomSecurityContextRepositoryConfig {
        companion object {
            var SECURITY_CONTEXT_REPOSITORY: ServerSecurityContextRepository = Mockito.mock(ServerSecurityContextRepository::class.java)
        }

        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                formLogin {
                    securityContextRepository = SECURITY_CONTEXT_REPOSITORY
                }
            }
        }
    }

    @Configuration
    open class UserDetailsConfig {
        @Bean
        open fun userDetailsService(): MapReactiveUserDetailsService {
            val user = User.withDefaultPasswordEncoder()
                    .username("user")
                    .password("password")
                    .roles("USER")
                    .build()
            return MapReactiveUserDetailsService(user)
        }
    }
}
