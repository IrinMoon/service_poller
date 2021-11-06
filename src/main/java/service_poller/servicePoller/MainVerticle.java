package service_poller.servicePoller;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  public void start(Promise<Void> startPromise){

    ConfigStoreOptions store = new ConfigStoreOptions()
            .setType("file")
            .setFormat("json")
            .setConfig(new JsonObject().put("path", "config.json"));

    ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(store);

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

    retriever.getConfig(asyncRes -> {
      if (asyncRes.succeeded()) {
        JsonObject config = asyncRes.result();
        logger.info("Retrieved configuration " + config);

        this.deployVerticles(startPromise, this.createDeploymentOptions(config));
      } else {
        logger.info("Failed to retrieve configuration");
      }
    });
  }

  private HashMap<String, DeploymentOptions> createDeploymentOptions(JsonObject config){
    DeploymentOptions httpOpts = new DeploymentOptions()
            .setConfig(new JsonObject().put("port", config.getJsonObject("http").getInteger("port")));

    JsonObject persistence = config.getJsonObject("persistence");
    DeploymentOptions persistenceOpts = new DeploymentOptions()
            .setConfig(new JsonObject()
                    .put("db_port", persistence.getInteger("db_port"))
                    .put("db_host", persistence.getString("db_host"))
                    .put("db_user", persistence.getString("db_user"))
                    .put("db_password", persistence.getString("db_password"))
                    .put("db_name", persistence.getString("db_name")));

    JsonObject poller = config.getJsonObject("poller");
    DeploymentOptions pollerOpts = new DeploymentOptions()
            .setConfig(new JsonObject()
                    .put("delay", poller.getInteger("delay"))
                    .put("timeout", poller.getInteger("timeout")));

    HashMap<String, DeploymentOptions> deploymentOptsHashMap = new HashMap<String, DeploymentOptions>();
    deploymentOptsHashMap.put("httpOpts", httpOpts);
    deploymentOptsHashMap.put("persistenceOpts", persistenceOpts);
    deploymentOptsHashMap.put("pollerOpts", pollerOpts);

    return deploymentOptsHashMap;
  }

  private void deployVerticles(Promise<Void> startPromise, HashMap<String, DeploymentOptions> deploymentOptsHashMap){

    Future<String> persistancePollerFuture = vertx.deployVerticle(new PersistenceVerticle(),
            deploymentOptsHashMap.get("persistenceOpts"))
            .compose(res -> vertx.deployVerticle(new PollerVerticle(), deploymentOptsHashMap.get("pollerOpts")));

    CompositeFuture.all(
            persistancePollerFuture,
            vertx.deployVerticle(new HTTPVerticle(), deploymentOptsHashMap.get("httpOpts"))
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
