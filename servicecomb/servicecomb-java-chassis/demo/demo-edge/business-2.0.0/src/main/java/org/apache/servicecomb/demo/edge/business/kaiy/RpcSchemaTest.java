package org.apache.servicecomb.demo.edge.business.kaiy;


import org.apache.servicecomb.provider.pojo.RpcSchema;

@RpcSchema(schemaId = "rpcSchemaTest")
public class RpcSchemaTest implements RpcSchemaTestInf {

    @Override
    public String rpcSchemaTest(String a,String b){
        return a+b;
    }

}
