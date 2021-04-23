package org.apache.servicecomb.demo.edge.consumer.kaiy.ext;

import org.apache.servicecomb.swagger.engine.SwaggerProducerOperation;
import org.apache.servicecomb.swagger.invocation.SwaggerInvocation;
import org.apache.servicecomb.swagger.invocation.extension.ProducerInvokeExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomProducerInvokeExtension implements ProducerInvokeExtension {

    public static Logger logger = LoggerFactory.getLogger(CustomProducerInvokeExtension.class);

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public <T> void beforeMethodInvoke(SwaggerInvocation invocation, SwaggerProducerOperation producerOperation, Object[] args) throws Exception {
        logger.info("进入CustomProducerInvokeExtension");
    }
}
