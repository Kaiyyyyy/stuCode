package org.apache.servicecomb.demo.edge.consumer.kaiy.ext;

import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.loadbalance.LoadBalancer;
import org.apache.servicecomb.loadbalance.ServerListFilterExt;
import org.apache.servicecomb.loadbalance.ServiceCombServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomServerListFilterExt implements ServerListFilterExt {

    static Logger logger = LoggerFactory.getLogger(CustomServerListFilterExt.class);

    private LoadBalancer loadBalancer;

    @Override
    public void setLoadBalancer(LoadBalancer loadBalancer) {

        this.loadBalancer = loadBalancer;
    }

    @Override
    public List<ServiceCombServer> getFilteredListOfServers(List<ServiceCombServer> servers, Invocation invocation) {
        logger.info("进入 CustomServerListFilterExt.getFilteredListOfServers");
        return servers;
    }
}
