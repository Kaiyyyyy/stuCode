package org.apache.servicecomb.demo.edge.consumer.kaiy.ext;

import org.apache.servicecomb.core.BootListener;
import org.apache.servicecomb.core.SCBEngine;
import org.apache.servicecomb.core.handler.ConsumerHandlerManager;
import org.apache.servicecomb.core.handler.ProducerHandlerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomBootListener implements BootListener {

    static Logger logger = LoggerFactory.getLogger(CustomBootListener.class);

    @Override
    public void onBeforeHandler(BootEvent event) {

    }

    @Override
    public void onAfterHandler(BootEvent event) {
        logger.info("进入 CustomBootListener.onAfterHandler");
        SCBEngine scbEngine = event.getScbEngine();
        ConsumerHandlerManager consumerHandlerManager = scbEngine.getConsumerHandlerManager();
        ProducerHandlerManager producerHandlerManager = scbEngine.getProducerHandlerManager();
//        consumerHandlerManager.

    }

    @Override
    public void onBeforeFilter(BootEvent event) {

    }

    @Override
    public void onAfterFilter(BootEvent event) {

    }

    @Override
    public void onBeforeProducerProvider(BootEvent event) {

    }

    @Override
    public void onAfterProducerProvider(BootEvent event) {

    }

    @Override
    public void onBeforeConsumerProvider(BootEvent event) {

    }

    @Override
    public void onAfterConsumerProvider(BootEvent event) {

    }

    @Override
    public void onBeforeTransport(BootEvent event) {

    }

    @Override
    public void onAfterTransport(BootEvent event) {

    }

    @Override
    public void onBeforeRegistry(BootEvent event) {

    }

    @Override
    public void onAfterRegistry(BootEvent event) {

    }

    @Override
    public void onBeforeClose(BootEvent event) {

    }

    @Override
    public void onAfterClose(BootEvent event) {

    }
}
