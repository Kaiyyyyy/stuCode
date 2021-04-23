package com.kaiy.cloud.alibaba.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class TestController {

    @Autowired
    private Environment environment;

    @RequestMapping("/get")
    public String get(){
        return environment.getProperty("spring.application.name");
    }

}
