package io.arsha.api.routes.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cache2k.Cache;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.V1Key;
import io.arsha.api.cache.V2Key;
import io.arsha.api.market.enums.MarketEndpoint;
import io.arsha.api.util.Util;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class V1 {
    public static void registerOperations(RouterBuilder api) {
        api.operation("basicHotList").handler(V1::GetWorldMarketHotList).failureHandler(Util::handleError);
        api.operation("basicMarketList").handler(V1::GetWorldMarketList).failureHandler(Util::handleError);
        api.operation("basicMarketSubList").handler(V1::GetWorldMarketSubList).failureHandler(Util::handleError);
        api.operation("basicMarketSearchList").handler(V1::GetWorldMarketSearchList).failureHandler(Util::handleError);

        api.operation("basicMarketBiddingList").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList)).failureHandler(Util::handleError);
        api.operation("basicMarketPriceInfo").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo)).failureHandler(Util::handleError);
        api.operation("basicPrice").handler(V1::price).failureHandler(Util::handleError);

        // Legacy
        api.operation("basicAliasItem").handler(V1::GetWorldMarketSubList).failureHandler(Util::handleError);
        api.operation("basicAliasOrders").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList)).failureHandler(Util::handleError);
        api.operation("basicAliasHistory").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo)).failureHandler(Util::handleError);
    }

    private static void GetWorldMarketHotList(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(region);
        Future<Buffer> cacheResponse = cache.get(new V1Key("x", "x", region, MarketEndpoint.GetWorldMarketHotList));
        cacheResponse.onSuccess(hotList -> {
            try {
                ctx.response().end(hotList.toJsonObject().encodePrettily());
            } catch (DecodeException json) {
                ctx.fail(513);
            }
        }).onFailure(fail -> {
            ctx.fail(500);
        });
    }

    private static void GetWorldMarketList(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        MultiMap params = ctx.request().params();
        String mainCategory = params.get("mainCategory");
        String subCategory = params.get("subCategory") == null ? "0" : params.get("subCategory");
        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(region);

        V1Key requestString = new V1Key(mainCategory, subCategory, region, MarketEndpoint.GetWorldMarketList);
        Future<Buffer> cacheResponse = cache.get(requestString);
        cacheResponse.onSuccess(marketList -> {
            try {
                ctx.response().end(marketList.toJsonObject().encodePrettily());
            } catch (DecodeException json) {
                json.printStackTrace();
                ctx.fail(513);
            }
        }).onFailure(fail -> {
            fail.printStackTrace();
            ctx.fail(500);
        });
    }

    private static void GetWorldMarketSubList(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(region);
        List<String> ids = ctx.request().params().getAll("id");
        List<Future> buffers = new ArrayList<>();
        ids.forEach(id -> {
            V1Key requestString =  new V1Key(id, "0", region, MarketEndpoint.GetWorldMarketSubList); // String.format("%s:%s:%s:%s", id, 0, region, 2);
            Future cacheResponse = cache.get(requestString);
            buffers.add(cacheResponse);
        });

        CompositeFuture.all(buffers).onSuccess(ar -> {
            JsonArray items = new JsonArray();
            for (Future<Buffer> buffer : buffers) items.add(buffer.result().toJsonObject());

            if (items.size() == 1) ctx.response().end(items.getJsonObject(0).encodePrettily());
            else ctx.response().end(items.encodePrettily());
        }).onFailure(fail -> ctx.fail(500));
    }

    private static void GetWorldMarketSearchList(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        MultiMap params = ctx.request().params();
        String ids = params.get("ids");
        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(region);

        V1Key requestString = new V1Key(ids, "x", region, MarketEndpoint.GetWorldMarketSearchList);// String.format("%s:%s:3", ids, region);
        Future<Buffer> cacheResponse = cache.get(requestString);
        cacheResponse.onSuccess(search -> {
            try {
                ctx.response().end(search.toJsonObject().encodePrettily());
            } catch (DecodeException json) {
                ctx.fail(513);
            }
        }).onFailure(fail -> ctx.fail(500));
    }

    private static void GetBiddingOrPriceInfo(RoutingContext ctx, MarketEndpoint requestId) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        MultiMap params = ctx.request().params();
        List<String> ids = params.getAll("id");
        List<String> sids = params.getAll("sid");

        if (sids.size() > 0 && (ids.size() != sids.size())) {
            ctx.fail(454);
            return;
        }

        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(region);
        List<Future> futures = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String sid = (sids.isEmpty() ? "0" : sids.get(i));

            V1Key requestString = new V1Key(id, sid, region, requestId); //String.format("%s:%s:%s:%s", id, sid, region, requestID);
            futures.add(cache.get(requestString));
        }

        CompositeFuture.all(futures).onSuccess(ar -> {
            JsonArray items = new JsonArray();
            for (Future<Buffer> buffer : futures) items.add(buffer.result().toJsonObject());

            if (items.size() == 1) ctx.response().end(items.getJsonObject(0).encodePrettily());
            else ctx.response().end(items.encodePrettily());
        }).onFailure(fail -> ctx.fail(500));
    }

    private static void price(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        MultiMap params = ctx.request().params();
        String id = params.get("id");
        String sid = params.get("sid") == null ? "0" : params.get("sid");

        String lang = Util.parseLang(ctx.request().getParam("lang"), region);
        Util.validateLang(ctx, lang);
        if (ctx.failed()) return;

        V2Key request = new V2Key(id, sid, region, MarketEndpoint.GetWorldMarketSubList, lang);
        Future<Buffer> sublistItem = CacheManager.getV2Cache(region).get(request);
        sublistItem.onSuccess(res -> {
            JsonArray result = res.toJsonArray();
            if (result.size() == 1 && !sid.equals("0"))  {
                ctx.fail(457);
                return;
            }

            if (result.getJsonObject(0).getString("name") == null) {
                ctx.fail(458);
                return;
            }

            Optional<Object> filter = result.stream().filter(obj -> ((JsonObject) obj).getInteger("sid") == Integer.valueOf(sid)).findFirst();
            if (filter.isPresent()) {
                JsonObject price = (JsonObject) filter.get(); 
                ctx.response().end(new JsonObject()
                    .put("name", price.getString("name"))
                    .put("id", price.getInteger("id"))
                    .put("sid", price.getInteger("sid"))
                    .put("basePrice", price.getLong("basePrice"))
                    .put("icon", price.getString("icon")).encodePrettily());
            } else {
                ctx.fail(458);
            }
        });
    }
    
}
