package org.apache.servicecomb.demo.edge.consumer.kaiy.ext;

import org.apache.servicecomb.common.rest.filter.HttpServerFilter;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
import org.apache.servicecomb.swagger.invocation.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomHttpServerFilter implements HttpServerFilter {

    static Logger logger = LoggerFactory.getLogger(CustomHttpServerFilter.class);

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Response afterReceiveRequest(Invocation invocation, HttpServletRequestEx requestEx) {
        logger.info("进入 HttpServerFilter.afterReceiveRequest()");
        return null;
    }

    @Override
    public void beforeSendResponse(Invocation invocation, HttpServletResponseEx responseEx) {
        logger.info("进入 HttpServerFilter.beforeSendResponse()");
    }
}
