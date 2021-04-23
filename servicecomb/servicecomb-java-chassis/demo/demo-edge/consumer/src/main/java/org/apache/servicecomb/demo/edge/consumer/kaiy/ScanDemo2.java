package org.apache.servicecomb.demo.edge.consumer.kaiy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ScanDemo2 {

    @Autowired
    private ScanDemo scanDemo;

    @PostConstruct
    public void init(){
        System.out.println(scanDemo);
        System.out.println("ScanDemo init");
    }
}
