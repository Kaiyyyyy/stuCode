package org.apache.servicecomb.demo.edge.consumer.kaiy;

import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.provider.springmvc.reference.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestSchema(schemaId = "kaiy")
@RequestMapping("/kaiy")
public class RestSchemaTestController {
//    RestTemplate restTemplate = RestTemplateBuilder.create();

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ConsumerTest consumerTest;

    @RequestMapping(value = "/test",method = RequestMethod.GET)
    @ResponseBody
    public String test(@RequestParam("param1")String param1){
        return "11";
    }

    @RequestMapping(value = "/testRpc",method = RequestMethod.GET)
    @ResponseBody
    public String testRpc(){
        return consumerTest.testRpc();
    }

    @RequestMapping(value = "/testRest",method = RequestMethod.GET)
    @ResponseBody
    public Object testRest(){
        String path ="cse://business/business/v2/add?x={x}&y={y}";
        Object forObject = restTemplate.getForObject(path, Object.class, 1, 2);
        return forObject;
    }
}
