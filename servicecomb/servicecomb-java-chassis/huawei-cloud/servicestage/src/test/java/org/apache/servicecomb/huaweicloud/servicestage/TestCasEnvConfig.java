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
package org.apache.servicecomb.huaweicloud.servicestage;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCasEnvConfig {

  @BeforeClass
  public static void init() {
    System.setProperty("servicecomb.huaweicloud.servicestage.cas.application-id", "application-id");
    System.setProperty("servicecomb.huaweicloud.servicestage.cas.environment-id", "env-id");
  }

  @Test
  public void testConfig() {
    CasEnvConfig instance = CasEnvConfig.INSTANCE;
    assertEquals(2, instance.getNonEmptyProperties().size());
    assertEquals("application-id", instance.getNonEmptyProperties().get("CAS_APPLICATION_ID"));
    assertEquals("env-id", instance.getNonEmptyProperties().get("CAS_ENVIRONMENT_ID"));
  }

  @AfterClass
  public static void destroy() {
    System.getProperties().remove("servicecomb.huaweicloud.servicestage.cas.application-id");
    System.getProperties().remove("servicecomb.huaweicloud.servicestage.cas.environment-id");
  }
}
