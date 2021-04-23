package com.kaiy.cloud.alibaba.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    @GetMapping("/user")
    public Authentication user(@AuthenticationPrincipal Authentication authentication){
        return authentication;
    }
}
