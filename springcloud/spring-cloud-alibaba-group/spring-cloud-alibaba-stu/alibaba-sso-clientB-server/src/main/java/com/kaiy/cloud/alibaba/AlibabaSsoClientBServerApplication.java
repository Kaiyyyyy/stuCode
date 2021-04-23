package com.kaiy.cloud.alibaba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;

/**
 * @author ASUS
 */
@SpringBootApplication
@EnableOAuth2Sso
public class AlibabaSsoClientBServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlibabaSsoClientBServerApplication.class,args);
    }
}
