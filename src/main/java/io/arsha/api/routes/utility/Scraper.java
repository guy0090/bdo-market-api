package io.arsha.api.routes.utility;

import java.util.ArrayList;
import java.util.List;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.UtilComposite;
import io.arsha.api.util.Util;
import io.arsha.api.util.mongodb.Mongo;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class Scraper {
  private static String apiKey = "";
  private static Vertx vertx = null;

  /**
   * Initialize and pass <code>Vertx</code> instance
   * 
   * @param vtx the <code>Vertx</code> instance
   * @return    <code>Future</code> with success or fail
   */
  public static Future<Void> init(Vertx vtx) {
    Promise<Void> init = Promise.promise();
    Util.read("api/key.txt").onSuccess(buffer -> {
      apiKey = buffer.toString();
      vertx = vtx;

      if (apiKey.equals("")) init.fail("API key is empty");
      else init.complete();
    }).onFailure(init::fail);
    return init.future();
  }

  public static Router getScraperRouter() {
    Router scraperRouter = Router.router(vertx);

    scraperRouter.post().handler(ctx -> {
      String key = ctx.request().getHeader("apikey");
      if (!key.equals(apiKey)) {
        ctx.fail(401);
      } else {
        if (ctx.request().getHeader("c") != null) {
          CacheManager.getFullDBCache().clear();
          System.gc();
          ctx.response().end();
        } else {
          List<Future> collections = new ArrayList<>();
          collections.add(Mongo.getItemClient().getCollections());
          collections.add(Mongo.getRecipeClient().getCollections());
        
          CompositeFuture.all(collections).onSuccess(cf -> {
            List<Future> caches = new ArrayList<>();
            for (Future<List<String>> db : collections) {
              db.result().forEach(result -> {
                UtilComposite query = new UtilComposite(result, new JsonObject());
                CacheManager.getFullDBCache().get(query);
              });           
            }

            CompositeFuture.all(caches)
              .onSuccess(cached -> ctx.response().end())
              .onFailure(fail -> ctx.fail(500));
          }).onFailure(fail -> ctx.fail(500));
        }
      }
    }).failureHandler(Util::handleError);
    return scraperRouter;
  }
}
