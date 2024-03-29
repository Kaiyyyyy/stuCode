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

package org.apache.servicecomb.transport.rest.vertx;

import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.servicecomb.common.accessLog.AccessLogConfig;
import org.apache.servicecomb.common.accessLog.core.element.impl.LocalHostAccessItem;
import org.apache.servicecomb.common.rest.codec.RestObjectMapperFactory;
import org.apache.servicecomb.core.Endpoint;
import org.apache.servicecomb.core.event.ServerAccessLogEvent;
import org.apache.servicecomb.core.transport.AbstractTransport;
import org.apache.servicecomb.foundation.common.event.EventManager;
import org.apache.servicecomb.foundation.common.net.URIEndpointObject;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.foundation.common.utils.ExceptionUtils;
import org.apache.servicecomb.foundation.common.utils.SPIServiceUtils;
import org.apache.servicecomb.foundation.ssl.SSLCustom;
import org.apache.servicecomb.foundation.ssl.SSLOption;
import org.apache.servicecomb.foundation.ssl.SSLOptionFactory;
import org.apache.servicecomb.foundation.vertx.VertxTLSBuilder;
import org.apache.servicecomb.foundation.vertx.metrics.DefaultHttpServerMetrics;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultServerEndpointMetric;
import org.apache.servicecomb.swagger.invocation.exception.CommonExceptionData;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicPropertyFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

public class RestServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestServerVerticle.class);

  private static final String SSL_KEY = "rest.provider";

  private Endpoint endpoint;

  private URIEndpointObject endpointObject;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.endpoint = (Endpoint) context.config().getValue(AbstractTransport.ENDPOINT_KEY);
    this.endpointObject = (URIEndpointObject) endpoint.getAddress();
  }

  /**
   * Verticle核心方法
   * @param startFuture
   * @throws Exception
   */
  @SuppressWarnings("deprecation")
  // TODO: vert.x 3.8.3 does not update startListen to promise, so we keep use deprecated API now. update in newer version.
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    try {
      super.start();
      // 如果本地未配置地址，则表示不必监听，只需要作为客户端使用即可
      if (endpointObject == null) {
        LOGGER.warn("rest listen address is not configured, will not start.");
        startFuture.complete();
        return;
      }
      Router mainRouter = Router.router(vertx);
      // 设置是否打印请求log
      mountAccessLogHandler(mainRouter);
      // 处理是否允许跨域
      mountCorsHandler(mainRouter);
      // 匹配对应分发器
      // 重要方法
      // 收集所有VertxHttpDispatcher，排序完后对Router进行添加Handle
      initDispatcher(mainRouter);
      //全局Rest失败的处理器，内置了默认逻辑，如果提供了GlobalRestFailureHandler，将使用自己提供的。
      mountGlobalRestFailureHandler(mainRouter);

      HttpServer httpServer = createHttpServer();
      httpServer.requestHandler(mainRouter);
      httpServer.connectionHandler(connection -> {
        DefaultHttpServerMetrics serverMetrics = (DefaultHttpServerMetrics) ((ConnectionBase) connection).metrics();
        DefaultServerEndpointMetric endpointMetric = serverMetrics.getEndpointMetric();
        long connectedCount = endpointMetric.getCurrentConnectionCount();
        int connectionLimit = DynamicPropertyFactory.getInstance()
            .getIntProperty("servicecomb.rest.server.connection-limit", Integer.MAX_VALUE).get();
        if (connectedCount > connectionLimit) {
          connection.close();
          endpointMetric.onRejectByConnectionLimit();
        }
      });
      // 获取所有HttpServerExceptionHandler，异常后依次获取执行其HttpServerExceptionHandler.handle()
      List<HttpServerExceptionHandler> httpServerExceptionHandlers =
          SPIServiceUtils.getAllService(HttpServerExceptionHandler.class);
      httpServer.exceptionHandler(e -> {
        if (e instanceof ClosedChannelException) {
          // This is quite normal in between browser and ege, so do not print out.
          LOGGER.debug("Unexpected error in server.{}", ExceptionUtils.getExceptionMessageWithoutTrace(e));
        } else {
          LOGGER.error("Unexpected error in server.{}", ExceptionUtils.getExceptionMessageWithoutTrace(e));
        }
        // 扩展点
        httpServerExceptionHandlers.forEach(httpServerExceptionHandler -> {
          httpServerExceptionHandler.handle(e);
        });
      });
      // 开启端口监听
      startListen(httpServer, startFuture);
    } catch (Throwable e) {
      // vert.x got some states that not print error and execute call back in VertexUtils.blockDeploy, we add a log our self.
      LOGGER.error("", e);
      throw e;
    }
  }

  /**
   * 全局Rest失败的处理器，内置了默认逻辑，如果提供了GlobalRestFailureHandler，将使用自己提供的。
   * @param mainRouter
   */
  private void mountGlobalRestFailureHandler(Router mainRouter) {
    GlobalRestFailureHandler globalRestFailureHandler =
        SPIServiceUtils.getPriorityHighestService(GlobalRestFailureHandler.class);
    Handler<RoutingContext> failureHandler = null == globalRestFailureHandler ?
        ctx -> {
          if (ctx.response().closed() || ctx.response().ended()) {
            // response has been sent, do nothing
            LOGGER.error("get a failure with closed response", ctx.failure());
            return;
          }
          HttpServerResponse response = ctx.response();
          if (ctx.failure() instanceof InvocationException) {
            // ServiceComb defined exception
            InvocationException exception = (InvocationException) ctx.failure();
            response.setStatusCode(exception.getStatusCode());
            response.setStatusMessage(exception.getReasonPhrase());
            response.end(exception.getErrorData().toString());
            return;
          }

          LOGGER.error("unexpected failure happened", ctx.failure());
          try {
            // unknown exception
            CommonExceptionData unknownError = new CommonExceptionData("unknown error");
            ctx.response().setStatusCode(500).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .end(RestObjectMapperFactory.getRestObjectMapper().writeValueAsString(unknownError));
          } catch (Exception e) {
            LOGGER.error("failed to send error response!", e);
          }
        }
        : globalRestFailureHandler;

    mainRouter.route()
        // this handler does nothing, just ensure the failure handler can catch exception
        .handler(RoutingContext::next)
        .failureHandler(failureHandler);
  }

  /**
   * 处理日志
   * @param mainRouter
   */
  private void mountAccessLogHandler(Router mainRouter) {
    if (!AccessLogConfig.INSTANCE.isServerLogEnabled()) {
      return;
    }
    LOGGER.info("access log enabled, pattern = {}", AccessLogConfig.INSTANCE.getServerLogPattern());
    mainRouter.route().handler(context -> {
      ServerAccessLogEvent accessLogEvent = new ServerAccessLogEvent()
          .setRoutingContext(context)
          .setMilliStartTime(System.currentTimeMillis())
          .setLocalAddress(LocalHostAccessItem.getLocalAddress(context));
      context.response().endHandler(event ->
          EventManager.post(accessLogEvent.setMilliEndTime(System.currentTimeMillis())));
      context.next();
    });
  }

  /**
   * Support CORS
   */
  void mountCorsHandler(Router mainRouter) {
    if (!TransportConfig.isCorsEnabled()) {
      return;
    }

    CorsHandler corsHandler = getCorsHandler(TransportConfig.getCorsAllowedOrigin());
    // Access-Control-Allow-Credentials
    corsHandler.allowCredentials(TransportConfig.isCorsAllowCredentials());
    // Access-Control-Allow-Headers
    corsHandler.allowedHeaders(TransportConfig.getCorsAllowedHeaders());
    // Access-Control-Allow-Methods
    Set<String> allowedMethods = TransportConfig.getCorsAllowedMethods();
    for (String method : allowedMethods) {
      corsHandler.allowedMethod(HttpMethod.valueOf(method));
    }
    // Access-Control-Expose-Headers
    corsHandler.exposedHeaders(TransportConfig.getCorsExposedHeaders());
    // Access-Control-Max-Age
    int maxAge = TransportConfig.getCorsMaxAge();
    if (maxAge >= 0) {
      corsHandler.maxAgeSeconds(maxAge);
    }

    LOGGER.info("mount CorsHandler");
    mainRouter.route().handler(corsHandler);
  }

  private CorsHandler getCorsHandler(String corsAllowedOrigin) {
    return CorsHandler.create(corsAllowedOrigin);
  }

  private void initDispatcher(Router mainRouter) {
    // 获取所有的VertxHttpDispatcher
    List<VertxHttpDispatcher> dispatchers = SPIServiceUtils.loadSortedService(VertxHttpDispatcher.class);
    BeanUtils.addBeans(VertxHttpDispatcher.class, dispatchers);
    
    for (VertxHttpDispatcher dispatcher : dispatchers) {
      if (dispatcher.enabled()) {
        dispatcher.init(mainRouter);
      }
    }
  }

  @SuppressWarnings("deprecation")
  // TODO: vert.x 3.8.3 does not update startListen to promise, so we keep use deprecated API now. update in newer version.
  private void startListen(HttpServer server, Future<Void> startFuture) {
    server.listen(endpointObject.getPort(), endpointObject.getHostOrIp(), ar -> {
      if (ar.succeeded()) {
        LOGGER.info("rest listen success. address={}:{}",
            endpointObject.getHostOrIp(),
            ar.result().actualPort());
        startFuture.complete();
        return;
      }

      String msg = String.format("rest listen failed, address=%s:%d",
          endpointObject.getHostOrIp(),
          endpointObject.getPort());
      LOGGER.error(msg, ar.cause());
      startFuture.fail(ar.cause());
    });
  }

  private HttpServer createHttpServer() {
    HttpServerOptions serverOptions = createDefaultHttpServerOptions();
    return vertx.createHttpServer(serverOptions);
  }

  private HttpServerOptions createDefaultHttpServerOptions() {
    HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setUsePooledBuffers(true);
    serverOptions.setIdleTimeout(TransportConfig.getConnectionIdleTimeoutInSeconds());
    serverOptions.setCompressionSupported(TransportConfig.getCompressed());
    serverOptions.setMaxHeaderSize(TransportConfig.getMaxHeaderSize());
    serverOptions.setMaxInitialLineLength(TransportConfig.getMaxInitialLineLength());
    if (endpointObject.isHttp2Enabled()) {
      serverOptions.setUseAlpn(TransportConfig.getUseAlpn())
          .setInitialSettings(new Http2Settings().setMaxConcurrentStreams(TransportConfig.getMaxConcurrentStreams()));
    }
    if (endpointObject.isSslEnabled()) {
      SSLOptionFactory factory =
          SSLOptionFactory.createSSLOptionFactory(SSL_KEY, null);
      SSLOption sslOption;
      if (factory == null) {
        sslOption = SSLOption.buildFromYaml(SSL_KEY);
      } else {
        sslOption = factory.createSSLOption();
      }
      SSLCustom sslCustom = SSLCustom.createSSLCustom(sslOption.getSslCustomClass());
      VertxTLSBuilder.buildNetServerOptions(sslOption, sslCustom, serverOptions);
    }

    return serverOptions;
  }
}
