package io.arsha.api.routes.utility;

import io.arsha.api.API;
import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.UtilComposite;
import io.arsha.api.util.Util;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utility {
  private static Logger logger = LoggerFactory.getLogger(Utility.class);

  /**
   * Register Util operations.
   *
   * @param api the <code>RouterBuilder</code> from OpenAPI specification
   */
  public static void registerOperations(RouterBuilder api) {
    // Single element operations
    api.operation("utilDB")
      .handler(Utility::getDbItem)
      .failureHandler(Util::handleError);
    api.operation("postUtilDB")
      .handler(Utility::getDbItem)
      .failureHandler(Util::handleError);

    api.operation("utilRecipeDB")
      .handler(Utility::getRecipe)
      .failureHandler(Util::handleError);
    api.operation("postUtilRecipeDB")
      .handler(Utility::getRecipe)
      .failureHandler(Util::handleError);

    api.operation("utilMRecipeDB")
      .handler(Utility::getMRecipe)
      .failureHandler(Util::handleError);
    api.operation("postUtilMRecipeDB")
      .handler(Utility::getMRecipe)
      .failureHandler(Util::handleError);

    api.operation("utilMatgroupDB")
      .handler(Utility::getRecipeMatGroup)
      .failureHandler(Util::handleError);
    api.operation("postUtilMatgroupDB")
      .handler(Utility::getRecipeMatGroup)
      .failureHandler(Util::handleError);

    api.operation("utilMMatgroupDB")
      .handler(Utility::getMRecipeMatGroup)
      .failureHandler(Util::handleError);
    api.operation("postUtilMMatgroupDB")
      .handler(Utility::getMRecipeMatGroup)
      .failureHandler(Util::handleError);

    // Full DB operations
    api.operation("utilDBDump")
      .handler(Utility::getItemDB)
      .failureHandler(Util::handleError);

    api.operation("utilRecipeDBDump")
      .handler(Utility::getRecipeDB)
      .failureHandler(Util::handleError);

    api.operation("utilMRecipeDBDump")
      .handler(Utility::getMRecipeDB)
      .failureHandler(Util::handleError);

    api.operation("utilMatgroupDBDump")
      .handler(Utility::getRecipeMatGroupsDB)
      .failureHandler(Util::handleError);

    api.operation("utilMMatgroupDBDump")
      .handler(Utility::getMRecipeMatGroupsDB)
      .failureHandler(Util::handleError);
  }

  // Individual DB items
  private static void getDbItem(RoutingContext ctx) {
    getDbElement(ctx, "");
  }

  private static void getRecipe(RoutingContext ctx) {
    getDbElement(ctx, "_recipes");
  }

  private static void getMRecipe(RoutingContext ctx) {
    getDbElement(ctx, "_mrecipes");
  }

  private static void getRecipeMatGroup(RoutingContext ctx) {
    getDbElement(ctx, "_recipes_matgroups");
  }

  private static void getMRecipeMatGroup(RoutingContext ctx) {
    getDbElement(ctx, "_mrecipes_matgroups");
  }

  // Full DBs
  private static void getItemDB(RoutingContext ctx) {
    getFullDb(ctx, "");
  }

  private static void getRecipeDB(RoutingContext ctx) {
    getFullDb(ctx, "_recipes");
  }

  private static void getMRecipeDB(RoutingContext ctx) {
    getFullDb(ctx, "_mrecipes");
  }

  private static void getRecipeMatGroupsDB(RoutingContext ctx) {
    getFullDb(ctx, "_recipes_matgroups");
  }

  private static void getMRecipeMatGroupsDB(RoutingContext ctx) {
    getFullDb(ctx, "_mrecipes_matgroups");
  }

  /**
  * Get a single or multiple elements from a MongoDB collection
  * and send result as <code>RoutingContext</code> response.
  *
  * @param ctx        the <code>RoutingContext</code>
  * @param collection the name of the collection without language prefix to get elements from
  *                   This value will be concatenated with the language supplied in <code>ctx</code>
  *                   parameters
  */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void getDbElement(RoutingContext ctx, String collection) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    RequestParameter langParam = (ctx.request().method() == HttpMethod.POST
        ? params.headerParameter("lang") : params.queryParameter("lang"));

    if (langParam != null && !Util.getLangs().contains(langParam.getString())) {
      ctx.fail(456);
      return;
    }

    String lang = "";
    if (langParam == null) {
      lang = "en" + collection;
    } else {
      lang = langParam.getString() + collection;
    }

    JsonArray idsParam = new JsonArray();
    if (ctx.request().method() == HttpMethod.POST) {
      idsParam = params.headerParameter("id").getJsonArray();
    } else {
      idsParam = params.queryParameter("id").getJsonArray();
    }

    List<String> ids = idsParam.stream()
        .map(String::valueOf)
        .collect(Collectors.toList());
    List<Future> db = new ArrayList<>();
    for (String id : ids) {
      UtilComposite util = new UtilComposite(lang, new JsonObject().put("id", Integer.valueOf(id)));
      db.add(CacheManager.getDbCache().get(util));
    }

    CompositeFuture.all(db).onSuccess(cf -> {
      JsonArray items = new JsonArray();
      for (int i = 0; i < db.size(); i++) {
        Future<JsonObject> item = (Future<JsonObject>) db.get(i);
        if (item.result() == null) {
          items.add(new JsonObject()
              .put("id", Integer.valueOf(ids.get(i)))
              .put("error", 452)
              .put("message", "Does not exist in database"));
        } else {
          item.result().remove("_id");
          items.add(item.result());
        }
      }
      if (items.isEmpty()) {
        ctx.fail(452);
      } else if (items.size() > 1) {
        ctx.response().end(items.encodePrettily());
        logger.info(Util.formatLog(ctx.request()));
      } else {
        ctx.response().end(items.getJsonObject(0).encodePrettily());
        logger.info(Util.formatLog(ctx.request()));
      }
    }).onFailure(fail -> ctx.fail(513));
  }

  /**
  * Get an entire collection from MongoDB - if no language parameter is found
  * all collections of that type (item or recipe) are returned.
  *
  * <p>The result is sent as <code>RoutingContext</code> response.
  *
  * @param ctx        the <code>RoutingContext</code>
  * @param collection the name of the collection (without language prefix) to get elements from
  *                   This value will be concatenated with the language supplied in <code>ctx</code>
  *                   parameters
  */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void getFullDb(RoutingContext ctx, String collection) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");
    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    RequestParameter lang = (ctx.request().method() == HttpMethod.POST
        ? params.headerParameter("lang") : params.queryParameter("lang"));

    if (lang != null && !Util.getLangs().contains(lang.getString())) {
      ctx.fail(456);
      return;
    }

    if (lang == null) {
      List<String> langs = Util.getLangs();
      Map<String, Future> dbs = new HashMap<>();
      langs.forEach(l -> {
        UtilComposite key = new UtilComposite(l + collection, new JsonObject());
        dbs.put(l + collection, CacheManager.getFullDbCache().get(key));
      });
      CompositeFuture.all(new ArrayList(dbs.values())).onSuccess(cf -> {
        JsonObject all = new JsonObject();
        dbs.forEach((k, v) -> {
          List<JsonObject> dbRes = (List<JsonObject>) v.result();
          List<JsonObject> items = new ArrayList<>();
          for (JsonObject item : dbRes) {
            item.remove("_id");
            items.add(item);
          }
          all.put(k, items);
        });
        ctx.response().end(all.encode());
      });
    } else {
      UtilComposite key = new UtilComposite(lang.getString() + collection, new JsonObject());
      Future<List<JsonObject>> itemDB = CacheManager.getFullDbCache().get(key);
      itemDB.onSuccess(future -> {
        List<JsonObject> localeDB = future;
        for (JsonObject item : localeDB) {
          item.remove("_id");
        }
        ctx.response().end(new JsonArray(localeDB).encode());
        logger.info(Util.formatLog(ctx.request()));
      }).onFailure(fail -> ctx.fail(512));
    }
  }
}
