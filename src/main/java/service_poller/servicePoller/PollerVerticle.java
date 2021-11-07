package service_poller.servicePoller;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.util.HashMap;

public class PollerVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(PollerVerticle.class);

  private HashMap<Integer, String> servicesIdToURL    = new HashMap<Integer, String>();
  private HashMap<String, String> servicesStatus      = new HashMap<String, String>();
  private HashMap<String, JsonObject> servicesDetails = new HashMap<String, JsonObject>();

  @Override
  public void start(Promise<Void> startPromise) {

    vertx.eventBus().consumer("api.services.poller.delete", this::deleteService);
    vertx.eventBus().consumer("api.services.poller.register", this::registerService);
    vertx.eventBus().consumer("api.services.poller.status", this::getStatus);

    vertx.eventBus().request("api.services.getAll","", reply ->{
      if(reply.succeeded()) {
        JsonArray rows = new JsonArray((String) reply.result().body());
        for (Object row : rows) {
          JsonObject row_json = (JsonObject) row;
          String url = row_json.getString("url");
          row_json.remove("url");
          servicesStatus.put(url, "PENDING");
          servicesDetails.put(url, row_json);
          servicesIdToURL.put(row_json.getInteger("id"), url);
        }
        if (rows.isEmpty()) {
          logger.info("DB query returned empty, No Services to pole");
        } else {
          logger.info("Poller successfully received data from DB");
        }

        vertx.setPeriodic(config().getInteger("delay"), this::poleServices);

        startPromise.complete();
        logger.info("Poller verticle is up");

      }
      else {
        startPromise.fail("Poller failed to get data from db");
      }
    });
  }


  private void poleServices(Long aLong) {
    servicesStatus.forEach((url, status) -> {
      WebClient.create(vertx).getAbs(url)
        .timeout(config().getInteger("timeout"))
        .send(result -> {
        if(result.succeeded() && result.result().statusCode() == 200){
          if (servicesStatus.containsKey(url)){
            servicesStatus.put(url, "OK");
            logger.info(url + " status " + status);
          }
        }
        else {
          if (servicesStatus.containsKey(url)) {
            servicesStatus.put(url, "FAIL");
            logger.info(url + " status " + status);
          }
        }
      });
    });
  }

  private void registerService(Message msg) {
    JsonArray res_arr = new JsonArray((String) msg.body());
    JsonObject row_json = res_arr.getJsonObject(0);
    String url = row_json.getString("url");

    servicesStatus.put(url, "PENDING");
    servicesDetails.put(url, row_json);
    servicesIdToURL.put(row_json.getInteger("id"), url);

    logger.info("Poller registered new service: " + row_json);
  }

  private void deleteService(Message msg) {
    Integer id = Integer.valueOf((String) msg.body());
    String url = servicesIdToURL.get(id);

    servicesStatus.remove(url);
    servicesDetails.remove(url);
    servicesIdToURL.remove(id);

    logger.info("Poller deleted service with id: " + id + " URL: " + url);
  }

  private void getStatus(Message msg) {
    JsonArray res = new JsonArray();

    servicesStatus.forEach((url, status) -> {
      JsonObject details = servicesDetails.get(url);
      res.add(new JsonObject()
        .put("id", details.getValue("id"))
        .put("name", details.getValue("name"))
        .put("url", url)
        .put("created_at", details.getValue("created_at"))
        .put("status", servicesStatus.get(url)));
    });
    msg.reply(Json.encodePrettily(res));
  }
}
