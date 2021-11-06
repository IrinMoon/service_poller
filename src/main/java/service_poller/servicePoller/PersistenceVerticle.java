package service_poller.servicePoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import java.net.MalformedURLException;
import io.vertx.core.Promise;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class PersistenceVerticle extends AbstractVerticle {

  private static final int DB_PORT = 5432;
  private static final String DB_HOST = "localhost";
  private static final String DB_USER = "postgres";
  private static final String DB_PASSWORD = "ConstantBecoming";
  private static final String DB_NAME = "ServicePollerDB";

  private static final Logger logger = LoggerFactory.getLogger(PersistenceVerticle.class);

  private PgPool pgPool;
  private DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;

  @Override
  public void start(Promise<Void> startPromise) {
    pgPool = PgPool.pool(vertx, new PgConnectOptions()
      .setHost(DB_HOST)
      .setPort(DB_PORT)
      .setUser(DB_USER)
      .setDatabase(DB_NAME)
      .setPassword(DB_PASSWORD), new PoolOptions());

    vertx.eventBus().consumer("api.services.getOne", this::getOneService);
    vertx.eventBus().consumer("api.services.getAll", this::getAllServices);
    vertx.eventBus().consumer("api.services.register", this::registerService);
    vertx.eventBus().consumer("api.services.persistence.delete", this::deleteService);

    startPromise.complete();
    logger.info("Persistence verticle is up, established connection with DB on host " + DB_HOST + " port " + DB_PORT);
  }

  private void executeQuery(Message msg, String query) {
    pgPool.preparedQuery(query)
      .execute()
      .onSuccess(rows -> {
        JsonArray rowsArray = new JsonArray();
        for (Row row : rows){
          rowsArray.add(new JsonObject()
            .put("id", row.getInteger("id"))
            .put("name", row.getString("name"))
            .put("url", row.getString("url"))
            .put("created_at", formatter.format((OffsetDateTime)row.getTemporal("created_at"))));
        }
        msg.reply(Json.encodePrettily(rowsArray));
        logger.info("Sucessfully executed " + query);
      })
      .onFailure(failure -> {
        msg.fail(1, failure.getMessage());
        logger.info("Failed to execute " + query);
      });
  }

  private void getAllServices(Message msg) {
    String query = "select * from services;";
    this.executeQuery(msg, query);
  }

  private void getOneService(Message msg) {
    String id = (String)msg.body();
    String query = String.format("select * from services where id = %s;", id);
    this.executeQuery(msg, query);
  }

  private void registerService(Message msg) {
    try {
      String name = ((JsonObject) msg.body()).getString("name");
      URL url = new URL(((JsonObject) msg.body()).getString("url"));
      String query = String.format("insert into services (\"name\", \"url\") values ('%s', '%s') returning *;",
        name, url);
      this.executeQuery(msg, query);
    } catch (MalformedURLException e) {
      msg.fail(1, "bad URL");
    }
  }

  private void deleteService(Message msg) {
    String id = (String)msg.body();
    String query = String.format("delete from services where id = %s;", id);
    this.executeQuery(msg, query);
  }
}
