package io.arsha.api.util.mongodb;

import io.arsha.api.util.Util;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

public class Mongo {
  private static JsonObject options = new JsonObject();
  private static Logger logger = LoggerFactory.getLogger(Mongo.class);
  private static MongoClient items;
  private static MongoClient recipes;

  /**
  * Initialize and pass <code>Vertx</code> instance.
  *
  * @param vertx the <code>Vertx</code> instance
  * @return      <code>Future</code> with success of fail
  */
  public static Future<Void> init(Vertx vertx) {
    Promise<Void> init = Promise.promise();
    Util.read("conf/mongo.json").onSuccess(mongoConf -> {
      options = mongoConf.toJsonObject();
      String connection = String.format(
          options.getString("connection"),
          options.getString("user"),
          options.getString("pw")
      );

      JsonObject itemConfig = new JsonObject()
          .put("connection_string", connection)
          .put("db_name", "items");
      JsonObject recipeConfig = new JsonObject()
          .put("connection_string", connection)
          .put("db_name", "recipes");

      try {
        items = MongoClient.create(vertx, itemConfig);
        recipes = MongoClient.create(vertx, recipeConfig);

        CompositeFuture.all(
            items.getCollections(),
            recipes.getCollections()
        ).onSuccess(done -> {
          logger.info("Database connection test succeeded");
          init.complete();
        }).onFailure(fail -> {
          init.fail("Database connection test failed: " + fail.getMessage());
        });
      } catch (Exception e) {
        init.fail("MongoDB: " + e.getMessage());
      }
    }).onFailure(init::fail);

    return init.future();
  }

  /**
  * Get the client for item database operations.
  *
  * @return <code>MongoClient</code> for item database
  */
  public static MongoClient getItemClient() {
    return items;
  }

  /**
  * Get the client for recipe database operations.
  *
  * @return <code>MongoClient</code> for recipe database
  */
  public static MongoClient getRecipeClient() {
    return recipes;
  }

  /**
  * Get all entries in an item collection.
  *
  * @param collection the name of the item collection
  * @return           <code>Future</code> with result
  *                   <code>List&lt;JsonObject&gt;</code> if found otherwise null
  */
  public static Future<List<JsonObject>> getItemDB(String collection) {
    return items.find(collection, new JsonObject());
  }

  /**
  * Get an entry in an item collection.
  *
  * @param collection the collection to query
  * @param doc        the query to check again
  * @return           <code>Future</code> with result
  *                   <code>JsonObject</code> if found otherwise null
  */
  public static Future<JsonObject> getItem(String collection, JsonObject doc) {
    return items.findOne(collection, doc, new JsonObject());
  }

  /**
  * Get all entries in a recipe collection.
  *
  * @param collection the name of the recipe collection
  * @return           <code>Future</code> with result
  *                   <code>List&lt;JsonObject&gt;</code> if found otherwise null
  */
  public static Future<List<JsonObject>> getRecipeDB(String collection) {
    return recipes.find(collection, new JsonObject());
  }

  /**
  * Get an entry in a recipe collection.
  *
  * @param collection the collection to query
  * @param doc        the query to check again
  * @return           <code>Future</code> with result
                      <code>JsonObject</code> if found otherwise null
  */
  public static Future<JsonObject> getRecipe(String collection, JsonObject doc) {
    return recipes.findOne(collection, doc, new JsonObject());
  }
}
