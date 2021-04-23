package org.apache.servicecomb.demo.edge.consumer.kaiy.ext;

import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomHandler implements Handler {

    public static Logger logger = LoggerFactory.getLogger(CustomHandler.class);

    @Override
    public void handle(Invocation invocation, AsyncResponse asyncResp) throws Exception {
        logger.info("进入CustomHandler");
        invocation.next(asyncResp::handle);
    }
}
