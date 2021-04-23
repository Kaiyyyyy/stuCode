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

package org.apache.servicecomb.config.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.servicecomb.config.ConfigUtil;
import org.apache.servicecomb.config.archaius.sources.ConfigCenterConfigurationSourceImpl;
import org.apache.servicecomb.config.archaius.sources.ConfigCenterConfigurationSourceImpl.UpdateHandler;
import org.apache.servicecomb.config.client.ConfigCenterClient.ConfigRefresh;
import org.apache.servicecomb.foundation.common.event.EventManager;
import org.apache.servicecomb.foundation.vertx.client.ClientPoolManager;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientWithContext;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientWithContext.RunHandler;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClients;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.eventbus.Subscribe;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

@SuppressWarnings("deprecation")
public class TestConfigCenterClient {
  @BeforeClass
  public static void setUpClass() {
    ConfigCenterConfig.setConcurrentCompositeConfiguration(ConfigUtil.createLocalConfig());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConnectServer(@Mocked ClientPoolManager<HttpClientWithContext> clientMgr) {
    HttpClients.mockClientPoolManager("config-center", clientMgr);

    HttpClientRequest request = Mockito.mock(HttpClientRequest.class);
    Mockito.when(request.method()).thenReturn(HttpMethod.GET);
    Mockito.when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    Buffer rsp = Mockito.mock(Buffer.class);
    Mockito.when(rsp.toJsonObject()).thenReturn(new JsonObject(
        "{\"instances\":[{\"status\":\"UP\",\"endpoints\":[\"rest:0.0.0.0:30103\"],\"hostName\":\"125292-0.0.0.0\",\"serviceName\":\"configServer\",\"https\":false}]}"));
    HttpClientResponse event = Mockito.mock(HttpClientResponse.class);
    Mockito.when(event.bodyHandler(Mockito.any(Handler.class))).then(invocation -> {
      Handler<Buffer> handler = invocation.getArgumentAt(0, Handler.class);
      handler.handle(rsp);
      return null;
    });
    Mockito.when(event.statusCode()).thenReturn(200);
    HttpClient httpClient = Mockito.mock(HttpClient.class);
    Mockito.when(
        httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Handler.class)))
        .then(invocation -> {
          Handler<HttpClientResponse> handler = invocation.getArgumentAt(3, Handler.class);
          handler.handle(event);
          return request;
        });
    new MockUp<HttpClientWithContext>() {
      @Mock
      public void runOnContext(RunHandler handler) {
        handler.run(httpClient);
      }
    };
    new MockUp<MemberDiscovery>() {
      @Mock
      public void refreshMembers(JsonObject members) {
        Assert.assertTrue(members.size() == 1);
      }
    };
    UpdateHandler updateHandler = new ConfigCenterConfigurationSourceImpl().new UpdateHandler();
    ConfigCenterClient cc = new ConfigCenterClient(updateHandler);
    cc.connectServer();
  }

  @Test
  public void testConnectRefreshModeTwo() {
    ConfigCenterClient cc2 = new ConfigCenterClient(null);
    boolean status = false;
    try {
      Deencapsulation.setField(cc2, "refreshMode", 2);
      cc2.connectServer();
    } catch (Exception e) {
      status = true;
    }
    Assert.assertFalse(status);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void testConfigRefreshModeOne(@Mocked ClientPoolManager clientMgr) {
    String version1 = refreshAndGetCurrentRevision(clientMgr, 200, "huawei");
    //test the sdk get and change the latestRevision
    Assert.assertEquals("huawei", version1);
    String version2 = refreshAndGetCurrentRevision(clientMgr, 304, "rkd");
    //test that when return code is 304, the sdk do not change the latestRevision
    Assert.assertNotEquals("rkd", version2);
    String version3 = refreshAndGetCurrentRevision(clientMgr, 200, "");
    //make sure the current version is not ""
    Assert.assertNotEquals("", version3);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private String refreshAndGetCurrentRevision(ClientPoolManager clientMgr, int statusCode, String version) {

    ConfigCenterConfigurationSourceImpl impl = new ConfigCenterConfigurationSourceImpl();
    UpdateHandler updateHandler = impl.new UpdateHandler();
    HttpClientRequest request = Mockito.mock(HttpClientRequest.class);
    Mockito.when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    Buffer rsp = Mockito.mock(Buffer.class);
    Mockito.when(rsp.toString())
        .thenReturn(String.format(
            "{\"application\":{\"3\":\"2\",\"aa\":\"1\"},\"vmalledge\":{\"aa\":\"3\"},\"revision\": { \"version\": \"%s\"} }",
            version));

    HttpClientResponse httpClientResponse = Mockito.mock(HttpClientResponse.class);
    Mockito.when(httpClientResponse.bodyHandler(Mockito.any(Handler.class))).then(invocation -> {
      Handler<Buffer> handler = invocation.getArgumentAt(0, Handler.class);
      handler.handle(rsp);
      return null;
    });
    Mockito.when(httpClientResponse.statusCode()).thenReturn(statusCode);

    HttpClient httpClient = Mockito.mock(HttpClient.class);
    Mockito.when(
        httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Handler.class)))
        .then(invocation -> {
          Handler<HttpClientResponse> handler = invocation.getArgumentAt(3, Handler.class);
          handler.handle(httpClientResponse);
          return request;
        });

    HttpClientWithContext httpClientWithContext = new MockUp<HttpClientWithContext>() {
      @Mock
      public void runOnContext(RunHandler handler) {
        handler.run(httpClient);
      }
    }.getMockInstance();

    HttpClients.mockClientPoolManager("config-center", clientMgr);

    new Expectations() {
      {
        clientMgr.findThreadBindClientPool();
        result = httpClientWithContext;
      }
    };

    ConfigCenterClient cc = new ConfigCenterClient(updateHandler);
    ParseConfigUtils parseConfigUtils = new ParseConfigUtils(updateHandler);
    MemberDiscovery memberdis = new MemberDiscovery(Arrays.asList("http://configcentertest:30103"));
    ConfigRefresh refresh = cc.new ConfigRefresh(parseConfigUtils, memberdis);
    Deencapsulation.setField(cc, "refreshMode", 1);
    refresh.run();
    String currentVersionInfo = Deencapsulation.getField(parseConfigUtils, "currentVersionInfo").toString();
    return currentVersionInfo;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void testConfigRefreshModeZero(@Mocked ClientPoolManager clientMgr) {
    ConfigCenterConfigurationSourceImpl impl = new ConfigCenterConfigurationSourceImpl();
    UpdateHandler updateHandler = impl.new UpdateHandler();
    HttpClientRequest request = Mockito.mock(HttpClientRequest.class);
    Mockito.when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    Buffer rsp = Mockito.mock(Buffer.class);
    Mockito.when(rsp.toString())
        .thenReturn("{\"application\":{\"2\":\"2\",\"aa\":\"1\"},\"vmalledge\":{\"aa\":\"3\"}}");

    HttpClientResponse httpClientResponse = Mockito.mock(HttpClientResponse.class);
    Mockito.when(httpClientResponse.bodyHandler(Mockito.any(Handler.class))).then(invocation -> {
      Handler<Buffer> handler = invocation.getArgumentAt(0, Handler.class);
      handler.handle(rsp);
      return null;
    });
    Mockito.when(httpClientResponse.statusCode()).thenReturn(200);

    Buffer buf = Mockito.mock(Buffer.class);
    Mockito.when(buf.toJsonObject()).thenReturn(new JsonObject(
        "{\"action\":\"UPDATE\",\"key\":\"vmalledge\",\"value\":\"{\\\"aa\\\":\\\"3\\\"}\"}"));
    WebSocket websocket = Mockito.mock(WebSocket.class);
    Mockito.when(websocket.handler(Mockito.any(Handler.class))).then(invocation -> {
      Handler<Buffer> handler = invocation.getArgumentAt(0, Handler.class);
      handler.handle(buf);
      return websocket;
    });
    HttpClient httpClient = Mockito.mock(HttpClient.class);
    Mockito.when(
        httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Handler.class)))
        .then(invocation -> {
          Handler<HttpClientResponse> handler = invocation.getArgumentAt(3, Handler.class);
          handler.handle(httpClientResponse);
          return request;
        });
    Mockito.when(httpClient.websocket(Mockito.anyInt(),
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(MultiMap.class),
        Mockito.any(Handler.class),
        Mockito.any(Handler.class)))
        .then(invocation -> {
          Handler<WebSocket> handler = invocation.getArgumentAt(4, Handler.class);
          handler.handle(websocket);
          return null;
        });

    HttpClientWithContext httpClientWithContext = new MockUp<HttpClientWithContext>() {
      @Mock
      public void runOnContext(RunHandler handler) {
        handler.run(httpClient);
      }
    }.getMockInstance();

    HttpClients.mockClientPoolManager("config-center", clientMgr);

    new Expectations() {
      {
        clientMgr.findThreadBindClientPool();
        result = httpClientWithContext;
      }
    };

    ConfigCenterClient cc = new ConfigCenterClient(updateHandler);
    ParseConfigUtils parseConfigUtils = new ParseConfigUtils(updateHandler);
    MemberDiscovery memberdis = new MemberDiscovery(Arrays.asList("http://configcentertest:30103"));
    ConfigRefresh refresh = cc.new ConfigRefresh(parseConfigUtils, memberdis);
    refresh.run();
    Map<String, Object> flatItems = Deencapsulation.getField(parseConfigUtils, "flatItems");
    Assert.assertEquals(2, flatItems.size());
    Deencapsulation.setField(cc, "refreshMode", 0);
    refresh.run();
  }

  public static class ConfigRefreshExceptionEvent {
    Map<String, String> map;

    public ConfigRefreshExceptionEvent(Map<String, String> map) {
      this.map = map;
    }

    @Subscribe
    public void testMsg(Object event) {
      if (event instanceof ConnFailEvent) {
        map.put("result", "Fail event trigger");
      }
      if (event instanceof ConnSuccEvent) {
        map.put("result", "Succ event trigger");
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConfigRefreshException(@Mocked ClientPoolManager<HttpClientWithContext> clientMgr,
      @Mocked HttpClientWithContext httpClientWithContext) {
    ConfigCenterConfigurationSourceImpl impl = new ConfigCenterConfigurationSourceImpl();
    Map<String, String> map = new HashMap<>();
    EventManager.register(new ConfigRefreshExceptionEvent(map));
    UpdateHandler updateHandler = impl.new UpdateHandler();
    HttpClientRequest request = Mockito.mock(HttpClientRequest.class);
    Mockito.when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    Buffer rsp = Mockito.mock(Buffer.class);
    Mockito.when(rsp.toString())
        .thenReturn("{\"application\":{\"2\":\"2\",\"aa\":\"1\"},\"vmalledge\":{\"aa\":\"3\"}}");

    HttpClientResponse event = Mockito.mock(HttpClientResponse.class);
    Mockito.when(event.bodyHandler(Mockito.any(Handler.class))).then(invocation -> {
      Handler<Buffer> handler = invocation.getArgumentAt(0, Handler.class);
      handler.handle(rsp);
      return null;
    });
    Mockito.when(event.statusCode()).thenReturn(400);
    Buffer buf = Mockito.mock(Buffer.class);
    Mockito.when(buf.toJsonObject()).thenReturn(new JsonObject(
        "{\"action\":\"UPDATE\",\"key\":\"vmalledge\",\"value\":\"{\\\"aa\\\":\\\"3\\\"}\"}"));
    HttpClient httpClient = Mockito.mock(HttpClient.class);
    Mockito.when(
        httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Handler.class)))
        .then(invocation -> {
          Handler<HttpClientResponse> handler = invocation.getArgumentAt(3, Handler.class);
          handler.handle(event);
          return request;
        });
    new MockUp<HttpClientWithContext>() {
      @Mock
      public void runOnContext(RunHandler handler) {
        handler.run(httpClient);
      }
    };
    new Expectations() {
      {
        clientMgr.findThreadBindClientPool();
        result = httpClientWithContext;
      }
    };
    ConfigCenterClient cc = new ConfigCenterClient(updateHandler);
    HttpClients.mockClientPoolManager("config-center", clientMgr);
    ParseConfigUtils parseConfigUtils = new ParseConfigUtils(updateHandler);
    MemberDiscovery memberdis = new MemberDiscovery(Arrays.asList("http://configcentertest:30103"));
    ConfigRefresh refresh = cc.new ConfigRefresh(parseConfigUtils, memberdis);
    refresh.run();
    Assert.assertEquals("Fail event trigger", map.get("result"));
    Mockito.when(event.statusCode()).thenReturn(200);
    refresh.run();
    Assert.assertEquals("Succ event trigger", map.get("result"));
  }

  @Test
  public void destroy() {
    ConfigCenterClient configCenterClient = new ConfigCenterClient(null);
    ScheduledExecutorService executor = Deencapsulation.getField(configCenterClient, "executor");
    ScheduledExecutorService heartbeatTask = Executors.newScheduledThreadPool(1);
    Deencapsulation.setField(configCenterClient, "heartbeatTask", heartbeatTask);

    Assert.assertFalse(executor.isShutdown());

    configCenterClient.destroy();

    Assert.assertTrue(executor.isShutdown());
    Assert.assertTrue(heartbeatTask.isShutdown());
  }
}
