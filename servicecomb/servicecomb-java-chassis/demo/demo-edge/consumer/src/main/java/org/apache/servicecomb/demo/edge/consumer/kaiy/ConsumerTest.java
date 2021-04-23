package org.apache.servicecomb.demo.edge.consumer.kaiy;

import org.apache.servicecomb.provider.pojo.RpcReference;
import org.springframework.stereotype.Component;

@Component
public class ConsumerTest {

    @RpcReference(microserviceName = "business",schemaId = "rpcSchemaTest")
    private RpcSchemaTestInf rpcSchemaTestInf;

    public String testRpc(){
        return rpcSchemaTestInf.rpcSchemaTest("1", "2");
    }

}
