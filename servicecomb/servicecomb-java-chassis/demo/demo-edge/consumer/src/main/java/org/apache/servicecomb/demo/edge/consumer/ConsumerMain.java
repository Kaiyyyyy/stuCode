/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.demo.edge.consumer;

import org.apache.servicecomb.config.ConfigUtil;
import org.apache.servicecomb.config.archaius.sources.MicroserviceConfigLoader;
import org.apache.servicecomb.demo.edge.model.ChannelRequestBase;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.foundation.common.utils.Log4jUtils;

import java.util.concurrent.*;

public class ConsumerMain {
  public static void main(String[] args) throws Exception {

    // 自定义设置spring扫描目录，或在classpath*:META-INF/spring/scb-core-bean.xml和classpath*:META-INF/spring/*.bean.xml
    // 配置相应扫描路径
    System.setProperty("scb-scan-package","com.kaiy");

    // 默认spring xml配置文件，默认是classpath*:META-INF/spring/scb-core-bean.xml和classpath*:META-INF/spring/*.bean.xml
    // 可自由扩展
    String[] defaultBeanResource = BeanUtils.DEFAULT_BEAN_RESOURCE;

//    ConfigUtil.addConfig();
    Log4jUtils.init();
    BeanUtils.init();
//    CompletableFuture.supplyAsync(()-> "111");
//    Thread thread = new Thread(() -> System.out.println("11"));
//    ExecutorService executorService = Executors.newSingleThreadExecutor();
//    Future<String> submit = executorService.submit(() -> "111");
//    CompletableFuture.supplyAsync(()->"1")
//            .thenApplyAsync((e)->"11")
//            .handle((s,e)->{
//              return "111";
//            })
//            .exceptionally(e->"1").join();
//    boolean done = submit.isDone();
//
//    //
//    new Consumer().testEncrypt();
//    new Consumer().invokeBusiness("cse://business/business/v1", new ChannelRequestBase());
//
//    System.out.println("Running api dispatcher.");
//    new Consumer().run("api");
//    System.out.println("Running rest dispatcher.");
//    new Consumer().run("rest");
//    System.out.println("Running url dispatcher.");
//    new Consumer().run("url");
//    System.out.println("Running http dispatcher.");
//    new Consumer().run("http");
//
//    System.out.println("All test case finished.");
  }
}
