package org.apache.servicecomb.demo.edge.consumer.kaiy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MvcTest {

    @GetMapping("mvcTest")
    public String mvcTest(@RequestParam("a") String a, @RequestParam("b") String b){
        return a+b;
    }
}
