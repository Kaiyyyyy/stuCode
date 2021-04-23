package com.kaiy.cloud.alibaba.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenStore redisTokenStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientDetailsService jdbcClientDetailsService;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

//        clients.withClientDetails(jdbcClientDetailsService);
        clients.inMemory()

                .withClient("gateway-client")
                .secret(passwordEncoder.encode("gateway-secret"))
                .scopes("all","message.read", "message.write")
                //配置访问token的有效期
                .accessTokenValiditySeconds(3600)
                //配置刷新token的有效期
                .refreshTokenValiditySeconds(864000)
                .redirectUris("http://localhost:8888/login/oauth2/code/gateway")
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "password")

                .and()
                .withClient("test")
                .secret(passwordEncoder.encode("test"))
                .scopes("all","message.read", "message.write")
                .autoApprove(true)
                //配置访问token的有效期
                .accessTokenValiditySeconds(3600)
                //配置刷新token的有效期
                .refreshTokenValiditySeconds(864000)
                .redirectUris("http://www.baidu.com")
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "password")

                .and()
                .withClient("webflux-client")
                .secret(passwordEncoder.encode("webflux-secret"))
                .scopes("all","message.read", "message.write")
                //配置访问token的有效期
                .accessTokenValiditySeconds(3600)
                //配置刷新token的有效期
                .refreshTokenValiditySeconds(864000)
                .redirectUris("http://localhost:8888/login/oauth2/code/webflux")
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "password")

                .and()
                .withClient("client1")
                .secret(passwordEncoder.encode("client1-secret"))
                .scopes("all","message.read", "message.write")
                //配置访问token的有效期
                .accessTokenValiditySeconds(3600)
                //配置刷新token的有效期
                .refreshTokenValiditySeconds(864000)
                .redirectUris("http://localhost:9989/authorized")
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "password")

                .and()
                .withClient("client-server")
                .secret(passwordEncoder.encode("client-server-secret"))
                .scopes("all","message.read", "message.write")
                //配置访问token的有效期
                .accessTokenValiditySeconds(3600)
                //配置刷新token的有效期
                .refreshTokenValiditySeconds(864000)
                .redirectUris("http://localhost:9979/login/oauth2/code/client-server")
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "password")

                .and()
                .withClient("sso-client")
                .secret(passwordEncoder.encode("sso-client-secret"))
                .scopes("all","message.read", "message.write")
                //配置访问token的有效期
                .accessTokenValiditySeconds(3600)
                //配置刷新token的有效期
                .refreshTokenValiditySeconds(864000)
                .autoApprove(true)
                .redirectUris("http://localhost:9969/login","http://localhost:9959/login")
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "password");

    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(authenticationManager)
                .tokenStore(redisTokenStore)
                //支持GET,POST请求;
                .allowedTokenEndpointRequestMethods(HttpMethod.GET,HttpMethod.POST);
//        endpoints.setClientDetailsService(jdbcClientDetailsService);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception{
        //允许表单认证
        security.allowFormAuthenticationForClients()
        .checkTokenAccess("isAuthenticated()");
    }


}
