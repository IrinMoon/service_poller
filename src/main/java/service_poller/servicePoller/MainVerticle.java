package service_poller.servicePoller;

import io.vertx.core.*;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);


  public void start(Promise<Void> startPromise){

    Future<String> persistancePollerFuture = vertx.deployVerticle(new PersistenceVerticle())
      .compose(res -> vertx.deployVerticle(new PollerVerticle()));

    CompositeFuture.all(
      persistancePollerFuture,
      vertx.deployVerticle(new HTTPVerticle())
    ).onComplete(res ->{
      if (res.succeeded()) {
        startPromise.complete();
        logger.info("All verticles are up");
      } else {
        startPromise.fail(res.cause());
        logger.info("Deployment failed: " + res.cause());
      }
    });
  }
}
