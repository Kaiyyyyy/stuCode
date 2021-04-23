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

import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.config.test.SpringTestRule
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.config.EnableWebFlux
import reactor.core.publisher.Mono

/**
 * Tests for [ServerOAuth2ClientDsl]
 *
 * @author Eleftheria Stein
 */
class ServerOAuth2ClientDslTests {
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
    fun `OAuth2 client when custom client registration repository then bean is not required`() {
        this.spring.register(ClientRepoConfig::class.java).autowire()
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class ClientRepoConfig {
        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                authorizeExchange {
                    authorize(anyExchange, authenticated)
                }
                oauth2Client {
                    clientRegistrationRepository = InMemoryReactiveClientRegistrationRepository(
                            CommonOAuth2Provider.GOOGLE
                                    .getBuilder("google").clientId("clientId").clientSecret("clientSecret")
                                    .build()
                    )
                }
            }
        }
    }

    @Test
    fun `OAuth2 client when authorization request repository configured then custom repository used`() {
        this.spring.register(AuthorizationRequestRepositoryConfig::class.java, ClientConfig::class.java).autowire()

        this.client.get()
                .uri {
                    it.path("/")
                            .queryParam(OAuth2ParameterNames.CODE, "code")
                            .queryParam(OAuth2ParameterNames.STATE, "state")
                            .build()
                }
                .exchange()

        verify(AuthorizationRequestRepositoryConfig.AUTHORIZATION_REQUEST_REPOSITORY).loadAuthorizationRequest(any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class AuthorizationRequestRepositoryConfig {
        companion object {
            var AUTHORIZATION_REQUEST_REPOSITORY = mock(ServerAuthorizationRequestRepository::class.java)
                    as ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest>
        }

        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                oauth2Client {
                    authorizationRequestRepository = AUTHORIZATION_REQUEST_REPOSITORY
                }
            }
        }
    }

    @Test
    fun `OAuth2 client when authentication converter configured then custom converter used`() {
        this.spring.register(AuthenticationConverterConfig::class.java, ClientConfig::class.java).autowire()

        `when`(AuthenticationConverterConfig.AUTHORIZATION_REQUEST_REPOSITORY.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(OAuth2AuthorizationRequest.authorizationCode()
                        .authorizationUri("https://example.com/login/oauth/authorize")
                        .clientId("clientId")
                        .redirectUri("/authorize/oauth2/code/google")
                        .build()))

        this.client.get()
                .uri {
                    it.path("/authorize/oauth2/code/google")
                            .queryParam(OAuth2ParameterNames.CODE, "code")
                            .queryParam(OAuth2ParameterNames.STATE, "state")
                            .build()
                }
                .exchange()

        verify(AuthenticationConverterConfig.AUTHENTICATION_CONVERTER).convert(any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class AuthenticationConverterConfig {
        companion object {
            var AUTHORIZATION_REQUEST_REPOSITORY = mock(ServerAuthorizationRequestRepository::class.java)
                    as ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest>
            var AUTHENTICATION_CONVERTER: ServerAuthenticationConverter = mock(ServerAuthenticationConverter::class.java)
        }

        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                oauth2Client {
                    authorizationRequestRepository = AUTHORIZATION_REQUEST_REPOSITORY
                    authenticationConverter = AUTHENTICATION_CONVERTER
                }
            }
        }
    }

    @Test
    fun `OAuth2 client when authentication manager configured then custom manager used`() {
        this.spring.register(AuthenticationManagerConfig::class.java, ClientConfig::class.java).autowire()

        `when`(AuthenticationManagerConfig.AUTHORIZATION_REQUEST_REPOSITORY.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(OAuth2AuthorizationRequest.authorizationCode()
                        .authorizationUri("https://example.com/login/oauth/authorize")
                        .clientId("clientId")
                        .redirectUri("/authorize/oauth2/code/google")
                        .build()))
        `when`(AuthenticationManagerConfig.AUTHENTICATION_CONVERTER.convert(any()))
                .thenReturn(Mono.just(TestingAuthenticationToken("a", "b", "c")))

        this.client.get()
                .uri {
                    it.path("/authorize/oauth2/code/google")
                            .queryParam(OAuth2ParameterNames.CODE, "code")
                            .queryParam(OAuth2ParameterNames.STATE, "state")
                            .build()
                }
                .exchange()

        verify(AuthenticationManagerConfig.AUTHENTICATION_MANAGER).authenticate(any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    open class AuthenticationManagerConfig {
        companion object {
            var AUTHORIZATION_REQUEST_REPOSITORY = mock(ServerAuthorizationRequestRepository::class.java)
                    as ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest>
            var AUTHENTICATION_CONVERTER: ServerAuthenticationConverter = mock(ServerAuthenticationConverter::class.java)
            var AUTHENTICATION_MANAGER: ReactiveAuthenticationManager = mock(ReactiveAuthenticationManager::class.java)
        }

        @Bean
        open fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                oauth2Client {
                    authorizationRequestRepository = AUTHORIZATION_REQUEST_REPOSITORY
                    authenticationConverter = AUTHENTICATION_CONVERTER
                    authenticationManager = AUTHENTICATION_MANAGER
                }
            }
        }
    }

    @Configuration
    open class ClientConfig {
        @Bean
        open fun clientRegistrationRepository(): ReactiveClientRegistrationRepository {
            return InMemoryReactiveClientRegistrationRepository(
                    CommonOAuth2Provider.GOOGLE
                            .getBuilder("google").clientId("clientId").clientSecret("clientSecret")
                            .build()
            )
        }
    }
}
