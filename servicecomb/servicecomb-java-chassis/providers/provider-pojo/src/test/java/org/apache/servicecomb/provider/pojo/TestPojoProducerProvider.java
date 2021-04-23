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

package org.apache.servicecomb.provider.pojo;

public class TestPojoProducerProvider {
//  @Test
//  public void testPojoProducerProviderAlreadyInited(@Injectable PojoProducers producers,
//      @Injectable PojoProducerMeta meta, @Mocked RegistryUtils utils,
//      @Injectable Microservice service) throws Exception {
//    List<PojoProducerMeta> producersMeta = new ArrayList<>();
//    producersMeta.add(meta);
//    Person bean = new Person();
//    new Expectations() {
//      {
//        producers.getProducers();
//        result = producersMeta;
//        meta.getInstance();
//        result = bean;
//      }
//    };
//    PojoProducerProvider provider = new PojoProducerProvider();
//    Deencapsulation.setField(provider, "producerSchemaFactory", factory);
//    Deencapsulation.setField(provider, "pojoProducers", producers);
//    provider.init();
//    // expectations done in Expectations
//  }
//
//  @Test
//  public void testPojoProducerProvider(@Injectable ProducerSchemaFactory factory,
//      @Injectable PojoProducers producers, @Injectable PojoProducerMeta meta, @Mocked RegistryUtils utils,
//      @Injectable Microservice service) throws Exception {
//    List<PojoProducerMeta> producersMeta = new ArrayList<>();
//    producersMeta.add(meta);
//    new Expectations() {
//      {
//        producers.getProducers();
//        result = producersMeta;
//        meta.getInstance();
//        result = null;
//        meta.getImplementation();
//        result = "pojo:org.apache.servicecomb.provider.pojo.Person";
//      }
//    };
//    PojoProducerProvider provider = new PojoProducerProvider();
//    Deencapsulation.setField(provider, "producerSchemaFactory", factory);
//    Deencapsulation.setField(provider, "pojoProducers", producers);
//    provider.init();
//    Assert.assertEquals(provider.getName(), "pojo");
//    // expectations done in Expectations
//  }
}
