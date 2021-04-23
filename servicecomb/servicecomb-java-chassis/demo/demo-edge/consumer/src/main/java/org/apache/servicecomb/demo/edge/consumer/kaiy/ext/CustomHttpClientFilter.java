package org.apache.servicecomb.demo.edge.consumer.kaiy.ext;

import org.apache.servicecomb.common.rest.filter.HttpClientFilter;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
import org.apache.servicecomb.swagger.invocation.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomHttpClientFilter implements HttpClientFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomHttpClientFilter.class);

    public CustomHttpClientFilter(){
        System.out.println("CustomHttpClientFilter");
    }
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void beforeSendRequest(Invocation invocation, HttpServletRequestEx requestEx) {
        LOGGER.info("CustomHttpClientFilter 执行 beforeSendRequest ");
        System.out.println("CustomHttpClientFilter 执行 beforeSendRequest ");
    }

    @Override
    public Response afterReceiveResponse(Invocation invocation, HttpServletResponseEx responseEx) {
        LOGGER.info("CustomHttpClientFilter 执行 afterReceiveResponse ");
        System.out.println("CustomHttpClientFilter 执行 afterReceiveResponse ");
        return null;
    }
}
