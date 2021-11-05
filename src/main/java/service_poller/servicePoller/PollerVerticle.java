package service_poller.servicePoller;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.util.HashMap;

public class PollerVerticle extends AbstractVerticle {

  //TODO: move to config file
  private static final int DELAY = 1000;
  private static final int TIMEOUT = 3000;

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
        JsonArray rows = new JsonArray((String)reply.result().body());
        for (Object row : rows){
          JsonObject row_json = (JsonObject)row;
          String url = row_json.getString("url");
          row_json.remove("url");
          servicesStatus.put(url, "PENDING");
          servicesDetails.put(url, row_json);
          servicesIdToURL.put(row_json.getInteger("id"), url);

        }
        System.out.println("SUCCESS!!! POLLER GOT STUFF FROM DB");
        System.out.println("ID TO URL: " + servicesIdToURL.toString());
      }
      else {
        System.out.println("POLLER FAILED TO GET SERVICES FROM DB");
      }
    });

    vertx.setPeriodic(DELAY, this::poleServices);

    startPromise.complete();
    System.out.println("POLLER VERTICLE UP");
  }

  private void poleServices(Long aLong) {
    servicesStatus.forEach((url, status) -> {
      WebClient.create(vertx).getAbs(url)
        .timeout(TIMEOUT)
        .send(result -> {
        if(result.succeeded() && result.result().statusCode() == 200){
          if (servicesStatus.containsKey(url)){
            servicesStatus.put(url, "OK");
          }
        }
        else {
          if (servicesStatus.containsKey(url)) {
            servicesStatus.put(url, "FAIL");
          }
        }
      });
    });
    //System.out.println(servicesStatus.toString());
  }

  private void registerService(Message msg) {
    JsonArray res_arr = new JsonArray((String) msg.body());
    JsonObject row_json = res_arr.getJsonObject(0);
    String url = row_json.getString("url");

    servicesStatus.put(url, "PENDING");
    servicesDetails.put(url, row_json);
    servicesIdToURL.put(row_json.getInteger("id"), url);

    System.out.println(String.format(">>>> POLLER ADD %s", msg.body()));
    System.out.println("AFTER POLLER ADDED " + servicesStatus.toString());
  }

  private void deleteService(Message msg) {
    Integer id = Integer.valueOf((String) msg.body());
    String url = servicesIdToURL.get(id);
    System.out.println("***** id " + id);
    System.out.println("***** url " + url);

    servicesStatus.remove(url);
    servicesDetails.remove(url);
    servicesIdToURL.remove(id);

    System.out.println("AFTER POLLER DELETED " + servicesStatus.toString());
    System.out.println(String.format(">>>> POLLER DELETE %s", msg.body()));
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
    System.out.println("OoOoOoOo " + res.toString());
    msg.reply(Json.encodePrettily(res));
  }
}
