package io.arsha.api.routes.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.UtilKey;
import io.arsha.api.util.Util;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;

public class Utility {
    public static void registerOperations(RouterBuilder api) {
        api.operation("utilDB").handler(Utility::getDBItem).failureHandler(Util::handleError);
        api.operation("utilRecipeDB").handler(Utility::getRecipe).failureHandler(Util::handleError);
        api.operation("utilMRecipeDB").handler(Utility::getMRecipe).failureHandler(Util::handleError);
        api.operation("utilMatgroupDB").handler(Utility::getRecipeMatGroup).failureHandler(Util::handleError);
        api.operation("utilMMatgroupDB").handler(Utility::getMRecipeMatGroup).failureHandler(Util::handleError);

        api.operation("utilDBDump").handler(Utility::getItemDB).failureHandler(Util::handleError);
        api.operation("utilRecipeDBDump").handler(Utility::getRecipeDB).failureHandler(Util::handleError);
        api.operation("utilMRecipeDBDump").handler(Utility::getMRecipeDB).failureHandler(Util::handleError);
        api.operation("utilMatgroupDBDump").handler(Utility::getRecipeMatGroupsDB).failureHandler(Util::handleError);
        api.operation("utilMMatgroupDBDump").handler(Utility::getMRecipeMatGroupsDB).failureHandler(Util::handleError);
    }

    // Individual DB items
    private static void getDBItem(RoutingContext ctx) {
        getDBElement(ctx, "");
    }

    private static void getRecipe(RoutingContext ctx) {
        getDBElement(ctx, "_recipes");
    }

    private static void getMRecipe(RoutingContext ctx) {
        getDBElement(ctx, "_mrecipes");
    }

    private static void getRecipeMatGroup(RoutingContext ctx) {
        getDBElement(ctx, "_recipes_matgroups");
    }
    
    private static void getMRecipeMatGroup(RoutingContext ctx) {
        getDBElement(ctx, "_mrecipes_matgroups");
    }

    // Full DBs
    private static void getItemDB(RoutingContext ctx) {
        getFullDB(ctx, "");
    }

    private static void getRecipeDB(RoutingContext ctx) {
        getFullDB(ctx, "_recipes");
    }

    private static void getMRecipeDB(RoutingContext ctx) {
        getFullDB(ctx, "_mrecipes");
    }

    private static void getRecipeMatGroupsDB(RoutingContext ctx) {
        getFullDB(ctx, "_recipes_matgroups");
    }

    private static void getMRecipeMatGroupsDB(RoutingContext ctx) {
        getFullDB(ctx, "_mrecipes_matgroups");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void getDBElement(RoutingContext ctx, String localeKey) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        MultiMap params = ctx.request().params();
        List<String> ids = params.getAll("id");
        String lang = ctx.request().getParam("lang") == null ? "" : ctx.request().getParam("lang");
        if (!lang.equals("") && !Util.getLangs().contains(lang)) {
            ctx.fail(456);
            return;
        }

        if (lang.equals("")) lang = "en"+localeKey;
        else lang = lang+localeKey;

        List<Future> db = new ArrayList<>();
        for (String id : ids) {
            UtilKey util = new UtilKey(lang, new JsonObject().put("id", Integer.valueOf(id)));
            db.add(CacheManager.getDBCache().get(util));
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
            if (items.isEmpty()) ctx.fail(452);
            else if (items.size() > 1) ctx.response().end(items.encodePrettily());
            else ctx.response().send(items.getJsonObject(0).encodePrettily());
        }).onFailure(fail -> ctx.fail(513));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void getFullDB(RoutingContext ctx, String localeKey) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");
        String lang = ctx.request().getParam("lang") == null ? "" : ctx.request().getParam("lang");
        if (!lang.equals("") && !Util.getLangs().contains(lang)) {
            ctx.fail(456);
            return;
        }

        if (lang.equals("")) {
            List<String> langs = Util.getLangs();
            Map<String, Future> dbs = new HashMap<>();
            langs.forEach(l -> {
                UtilKey key = new UtilKey(l+localeKey, new JsonObject());
                dbs.put(l+localeKey, CacheManager.getFullDBCache().get(key));
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
            UtilKey key = new UtilKey(lang+localeKey, new JsonObject());
            Future<List<JsonObject>> itemDB = CacheManager.getFullDBCache().get(key);
            itemDB.onSuccess(future -> {
                List<JsonObject> localeDB = future;
                for (JsonObject item : localeDB) item.remove("_id");
                ctx.response().send(new JsonArray(localeDB).encode());
            }).onFailure(fail -> ctx.fail(512));
        }
    }
}
