package service_poller.servicePoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class HTTPVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(HTTPVerticle.class);

  @Override
  public void start(Promise<Void> startPromise){
    Router baseRouter = Router.router(vertx);
    Router apiRouter = Router.router(vertx);

    baseRouter.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain").end("Hello Service Poller");
    });

    apiRouter.route().handler(LoggerHandler.create());
    apiRouter.route("/services/*").handler(BodyHandler.create());


    apiRouter.post("/services").handler(this::registerService);
    apiRouter.delete("/services/:id").handler(this::deleteService);
    apiRouter.get("/services/status").handler(this::getStatus);
    apiRouter.get("/services/:id").handler(this::getOneService);
    apiRouter.get("/services").handler(this::getAllServices);


    baseRouter.mountSubRouter("/api", apiRouter);
    baseRouter.get().handler(StaticHandler.create());

    int port = config().getInteger("port");

    vertx.createHttpServer().requestHandler(baseRouter)
      .listen(port, result -> {
      if (result.succeeded()) {
        startPromise.complete();
        logger.info("HTTP verticle is up, listening to port " + port);

      } else {
        startPromise.fail(result.cause());
        logger.info("HTTP verticle failed to listen to port " + port);
      }
    });
  }

  private void registerService(RoutingContext routingContext) {
    JsonObject body = routingContext.getBodyAsJson();
    vertx.eventBus().request("api.services.register", body, reply ->{
      if(reply.succeeded()) {
        vertx.eventBus().send("api.services.poller.register", reply.result().body());
        routingContext.request().response().end((String) reply.result().body());
      }
      else {
        routingContext.request().response()
        .setStatusCode(400)
        .putHeader("Content-Type", "application/json; charset=utf-8")
        .end(reply.cause().getMessage());
      }
    });
  }

  private void deleteService(RoutingContext routingContext) {
    String id = routingContext.pathParam("id");

    vertx.eventBus().request("api.services.persistence.delete", id, reply ->{
      if(reply.succeeded()) {
        vertx.eventBus().send("api.services.poller.delete", id);
        routingContext.request().response().end((String) reply.result().body());
      }
      else {
        routingContext.request().response()
          .setStatusCode(400)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(reply.cause().getMessage());
      }
    });
  }

  private void getOneService(RoutingContext routingContext) {
    String id = routingContext.pathParam("id");
    vertx.eventBus().request("api.services.getOne", id, reply ->{
      if(reply.succeeded()) {
        routingContext.request().response().end((String) reply.result().body());
      }
      else {
        routingContext.request().response()
          .setStatusCode(400)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(reply.cause().getMessage());
      }
    });
  }

  private void getAllServices(RoutingContext routingContext) {
    vertx.eventBus().request("api.services.getAll","", reply ->{
      if(reply.succeeded()) {
        routingContext.request().response().end((String) reply.result().body());
      }
      else {
        routingContext.request().response()
          .setStatusCode(400)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(reply.cause().getMessage());
      }
    });
  }

  private void getStatus(RoutingContext routingContext) {
    vertx.eventBus().request("api.services.poller.status","", reply ->{
      if(reply.succeeded()) {
        routingContext.request().response().end((String) reply.result().body());
      }
      else {
        routingContext.request().response()
          .setStatusCode(400)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(reply.cause().getMessage());
      }
    });
  }
}
