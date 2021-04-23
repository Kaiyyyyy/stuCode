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
package org.apache.servicecomb.huaweicloud.dtm;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DtmConsumerHandler implements Handler {
  private static final Logger LOG = LoggerFactory.getLogger(DtmConsumerHandler.class);

  private Method dtmContextExMethod;

  public DtmConsumerHandler() {
    String className = DtmConfig.INSTANCE.getDtmContextClassName();
    try {
      Class<?> clazz = Class.forName(className);
      dtmContextExMethod = clazz.getMethod(DtmConfig.DTM_EXPORT_METHOD);
    } catch (Throwable e) {
      // ignore just warn
      LOG.warn("Failed to init method {}#{}", className, DtmConfig.DTM_EXPORT_METHOD, e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handle(Invocation invocation, AsyncResponse asyncResp) throws Exception {
    try {
      if (dtmContextExMethod != null) {
        Object context = dtmContextExMethod.invoke(null);
        if (context instanceof Map) {
          invocation.getContext().putAll((Map<? extends String, ? extends String>) context);
        }
      }
    } catch (Throwable e) {
      LOG.warn("Failed to execute method DTMContext#{}, please check", DtmConfig.DTM_EXPORT_METHOD, e);
    }
    invocation.next(asyncResp);
  }
}
