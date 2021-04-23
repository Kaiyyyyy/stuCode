package com.kaiy.cloud.alibaba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;

/**
 * @author ASUS
 */
@SpringBootApplication
@EnableOAuth2Sso
public class AlibabaSsoClientAServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlibabaSsoClientAServerApplication.class,args);
    }
}
