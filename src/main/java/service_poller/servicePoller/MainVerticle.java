package service_poller.servicePoller;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {
  //TODO: add logger
  //TODO: add config file
  //TODO: load testing (Jmeter?)
  //TODO: pack it all up in a container (Docker)?
  //TODO: readme how to deploy and run
  public void start(Promise<Void> startPromise){

    Future<String> persistancePollerFuture = vertx.deployVerticle(new PersistanceVerticle())
      .compose(res -> vertx.deployVerticle(new PollerVerticle()));

    CompositeFuture.all(
      persistancePollerFuture,
      vertx.deployVerticle(new HTTPVerticle())
    ).onComplete(res ->{
      if (res.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail(res.cause());
      }
    });
  }
}
