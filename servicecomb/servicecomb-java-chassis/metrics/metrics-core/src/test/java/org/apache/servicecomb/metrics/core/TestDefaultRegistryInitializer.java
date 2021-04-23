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
package org.apache.servicecomb.metrics.core;

import java.util.List;

import org.apache.servicecomb.foundation.metrics.MetricsBootstrapConfig;
import org.apache.servicecomb.foundation.metrics.registry.GlobalRegistry;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.spectator.api.Registry;

import mockit.Deencapsulation;

public class TestDefaultRegistryInitializer {
  GlobalRegistry globalRegistry = new GlobalRegistry();

  List<Registry> registries = Deencapsulation.getField(globalRegistry, "registries");

  DefaultRegistryInitializer registryInitializer = new DefaultRegistryInitializer();

  @Test
  @SuppressWarnings("deprecation")
  public void init() {
    registryInitializer.init(globalRegistry, new EventBus(), new MetricsBootstrapConfig());

    Assert.assertEquals(-10, registryInitializer.getOrder());
    Assert.assertThat(globalRegistry.getDefaultRegistry(),
        Matchers.instanceOf(com.netflix.spectator.servo.ServoRegistry.class));
    Assert.assertEquals(1, registries.size());
    Assert.assertEquals(1, DefaultMonitorRegistry.getInstance().getRegisteredMonitors().size());

    registryInitializer.destroy();

    Assert.assertEquals(0, registries.size());
    Assert.assertEquals(0, DefaultMonitorRegistry.getInstance().getRegisteredMonitors().size());
  }

  @Test
  public void destroy_notInit() {
    // should not throw exception
    registryInitializer.destroy();
  }
}
