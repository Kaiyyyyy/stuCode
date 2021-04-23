package org.apache.servicecomb.demo.edge.consumer.kaiy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ScanDemo {

    @Autowired
    private ScanDemo2 scanDemo2;

    @PostConstruct
    public void init(){
        System.out.println(scanDemo2);
        System.out.println("ScanDemo init");
    }
}
