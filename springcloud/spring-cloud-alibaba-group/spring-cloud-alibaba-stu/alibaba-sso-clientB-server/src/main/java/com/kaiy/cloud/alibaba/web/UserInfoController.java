package com.kaiy.cloud.alibaba.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    @Autowired
    private Environment environment;

    @GetMapping("/user")
    public Authentication user(@AuthenticationPrincipal Authentication authentication){
        return authentication;
    }

    @GetMapping("/client")
    public String getClient(){
        return environment.getProperty("spring.application.name");
    }
}
