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

package org.apache.servicecomb.core.definition;

public class TestMicroserviceVersionMeta {
//  @AfterClass
//  public static void teardown() {
//    CseContext.getInstance().setConsumerSchemaFactory(null);
//    CseContext.getInstance().setSchemaListenerManager(null);
//  }
//
//  @Test
//  public void construct(@Mocked ServiceRegistry serviceRegistry) {
//    String microserviceName = "app:ms";
//    String microserviceId = "id";
//    Microservice microservice = new Microservice();
//    microservice.setVersion("1.0.0");
//
//    new Expectations(RegistryUtils.class) {
//      {
//        RegistryUtils.getServiceRegistry();
//        result = serviceRegistry;
//        serviceRegistry.getAggregatedRemoteMicroservice(microserviceId);
//        result = microservice;
//      }
//    };
//
//    List<String> logs = new ArrayList<>();
//    CseContext.getInstance().setConsumerSchemaFactory(new MockUp<ConsumerSchemaFactory>() {
//      @Mock
//      void createConsumerSchema(MicroserviceMeta microserviceMeta, Microservice microservice) {
//        logs.add("createConsumerSchema");
//      }
//    }.getMockInstance());
//    CseContext.getInstance().setSchemaListenerManager(new MockUp<SchemaListenerManager>() {
//      @Mock
//      void notifySchemaListener(MicroserviceMeta... microserviceMetas) {
//        logs.add("notifySchemaListener");
//      }
//    }.getMockInstance());
//
//    MicroserviceVersionMeta microserviceVersionMeta = new MicroserviceVersionMeta(microserviceName, microserviceId);
//
//    Assert.assertThat(logs, Matchers.contains("createConsumerSchema", "notifySchemaListener"));
//    Assert.assertEquals(microserviceName, microserviceVersionMeta.getMicroserviceMeta().getName());
//  }
}
