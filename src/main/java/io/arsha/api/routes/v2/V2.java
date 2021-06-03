package io.arsha.api.routes.v2;

import java.util.ArrayList;
import java.util.List;

import org.cache2k.Cache;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.UtilKey;
import io.arsha.api.cache.V1Key;
import io.arsha.api.cache.V2Key;
import io.arsha.api.market.enums.MarketEndpoint;
import io.arsha.api.market.items.History;
import io.arsha.api.market.items.HotListItem;
import io.arsha.api.market.items.Item;
import io.arsha.api.market.items.ListItem;
import io.arsha.api.market.items.Order;
import io.arsha.api.market.items.SearchItem;
import io.arsha.api.util.Util;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class V2 {
    public static void registerOperations(RouterBuilder api) {
        api.operation("parsedHotList").handler(V2::GetWorldMarketHotList).failureHandler(Util::handleError);
        api.operation("parsedMarketList").handler(V2::GetWorldMarketList).failureHandler(Util::handleError);
        api.operation("parsedMarketSubList").handler(V2::GetWorldMarketSubList).failureHandler(Util::handleError);
        api.operation("parsedMarketSearchList").handler(V2::GetWorldMarketSearchList).failureHandler(Util::handleError);

        api.operation("parsedMarketBiddingList").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList)).failureHandler(Util::handleError);
        api.operation("parsedMarketPriceInfo").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo)).failureHandler(Util::handleError);

        // Legacy
        api.operation("parsedAliasItem").handler(V2::GetWorldMarketSubList).failureHandler(Util::handleError);
        api.operation("parsedAliasOrders").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList)).failureHandler(Util::handleError);
        
        api.operation("parsedAliasHistory").handler(ctx -> GetBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo)).failureHandler(Util::handleError);
    }

    /**
     * Get current hot list
     * 
     * @param request the <code>V2Key</code> composite key 
     * @return        <code>Future&lt;Buffer&gt;</code> of the list to grab
     */
    public static Future<Buffer> getHotList(V2Key request) {
        Promise<Buffer> response = Promise.promise();
        Future<Buffer> hotlist = CacheManager.getV1Cache(request.getRegion()).get(request.getChild());
        
        hotlist.onSuccess(list -> {
            List<Future> dbFutures = new ArrayList<>();
            List<HotListItem> hotlistItems = new ArrayList<>();
            JsonObject res = hotlist.result().toJsonObject();
            for (String subItem : res.getString("resultMsg").split("[|]")) {
                HotListItem i = new HotListItem(subItem.split("[-]"));
                hotlistItems.add(i);
                UtilKey util = new UtilKey(request.getLang(), new JsonObject().put("id", i.getId()));
                dbFutures.add(CacheManager.getDBCache().get(util));
            }

            CompositeFuture.all(dbFutures).onSuccess(cf -> {
                JsonArray items = new JsonArray();
                for (int i = 0; i < hotlistItems.size(); i++) {
                    JsonObject dbItem = (JsonObject) dbFutures.get(i).result();
                    HotListItem item = hotlistItems.get(i);
                    String prefix = Util.getItemPrefix(item.getId(), item.getMinEnhance(), item.getMaxEnhance());

                    item.setName(prefix + dbItem.getString("name"));
                    item.setIcon(dbItem.getString("icon"));
                    items.add(item.toJson());
                }
                response.complete(items.toBuffer());
            }).onFailure(response::fail);
        });
        return response.future();
    }

    /**
     * Get all items in specified (sub)category
     * 
     * @param request the <code>V2Key</code> list composite key 
     * @return        <code>Future&lt;Buffer&gt;</code> of the list to grab
     */
    public static Future<Buffer> getMarketList(V2Key request) {
        Promise<Buffer> response = Promise.promise();
        Future<Buffer> marketList = CacheManager.getV1Cache(request.getRegion()).get(request.getChild());

        marketList.onSuccess(list -> {
            JsonArray res = new JsonArray();
            String resultMsg = list.toJsonObject().getString("resultMsg");
            if (resultMsg.equals("0")) {
                response.complete(res.toBuffer());
            } else {
                for (String listItem : resultMsg.split("[|]")) {
                    res.add(new ListItem(listItem.split("[-]")).toJson());
                }
                response.complete(res.toBuffer());
            }
        }).onFailure(response::fail);

        return response.future();
    }

    /**
     * Get information for item and variants (enhancement levels)
     * 
     * @param request the <code>V2Key</code> item composite key 
     * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
     */
    public static Future<Buffer> getSubListItem(V2Key request) {
        Promise<Buffer> response = Promise.promise();
        Future<Buffer> itemFuture = CacheManager.getV1Cache(request.getRegion()).get(request.getChild());
        UtilKey util = new UtilKey(request.getLang(), new JsonObject().put("id", Integer.valueOf(request.getId())));
        Future<JsonObject> dbFuture = CacheManager.getDBCache().get(util);

        CompositeFuture.all(itemFuture, dbFuture).onSuccess(cf -> {
            JsonObject res = itemFuture.result().toJsonObject();
            JsonObject db = dbFuture.result();

            if (res.getString("resultMsg").equals("0")) {
                JsonObject item = new Item(request.getId(), request.getSid()).toJson();
                response.complete(new JsonArray().add(item).toBuffer());                
            } else {
                JsonArray items = new JsonArray();
                for (String subItem : res.getString("resultMsg").split("[|]")) {
                    Item i = new Item(subItem.split("[-]"));
                    String prefix = Util.getItemPrefix(i.getId(), i.getMinEnhance(), i.getMaxEnhance());
                    i.setName(prefix + db.getString("name"));
                    i.setIcon(db.getString("icon"));
                    items.add(i.toJson());
                }
                response.complete(items.toBuffer());
            }
        }).onFailure(response::fail);

        return response.future();
    }

    /**
     * Get item search information
     * 
     * @param request the <code>V2Key</code> item composite key 
     * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
     */
    public static Future<Buffer> getSearchItem(V2Key request) {
        Promise<Buffer> response = Promise.promise();
        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
        cache.get(request.getChild()).onSuccess(result -> {
            JsonObject asJson = result.toJsonObject();
            if (asJson.getString("resultMsg").equals("0")) {
                response.complete(new SearchItem(request.getId()).toJson().toBuffer());
            } else {
                String[] details = asJson.getString("resultMsg").split("[|]");
                response.complete(new SearchItem(details[0].split("[-]")).toJson().toBuffer());
            }
        }).onFailure(response::fail);

        return response.future();
    }

    /** 
     * Get item order information
     * 
     * @param request the <code>V2Key</code> item composite key 
     * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
     */
    public static Future<Buffer> getBiddingList(V2Key request) {
        Promise<Buffer> response = Promise.promise();
        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
        cache.get(request.getChild()).onSuccess(result -> {
            JsonObject asJson = result.toJsonObject();
            if (asJson.getString("resultMsg").equals("0") || asJson.getInteger("resultCode") == 8) {
                JsonObject order = new JsonObject()
                    .put("id", request.getId())
                    .put("sid", request.getSid())
                    .put("orders", new JsonArray());
                response.complete(order.toBuffer());
            } else {
                String[] details = asJson.getString("resultMsg").split("[|]");
                JsonArray orders = new JsonArray();
                for (String detail : details) orders.add(new Order(detail.split("[-]")).toJson());
                JsonObject res = new JsonObject()
                    .put("id", request.getId())
                    .put("sid", request.getSid())
                    .put("orders", orders);
                response.complete(res.toBuffer());
            }
        }).onFailure(response::fail);

        return response.future();
    }

    /**
     * Get item price history information
     * 
     * @param request the <code>V2Key</code> item composite key 
     * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
     */
    public static Future<Buffer> getPriceInfo(V2Key request) {
        Promise<Buffer> response = Promise.promise();
        Cache<V1Key, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
        cache.get(request.getChild()).onSuccess(result -> {
            JsonObject asJson = result.toJsonObject();
            if (asJson.getString("resultMsg").equals("0") || asJson.getInteger("resultCode") == 8) {
                JsonObject history = new JsonObject()
                    .put("id", request.getId())
                    .put("sid", request.getSid())
                    .put("history", new JsonObject());
                response.complete(history.toBuffer());
            } else {
                History history = Util.parseHistory(asJson.getString("resultMsg"), request.getRegion());
                JsonObject res = new JsonObject()
                    .put("id", request.getId())
                    .put("sid", request.getSid())
                    .put("history", history.toJson().getJsonObject("history"));
                response.complete(res.toBuffer());
            }
        }).onFailure(response::fail);

        return response.future();
    }   

    private static void GetWorldMarketHotList(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        String lang = Util.parseLang(ctx.request().getParam("lang"), region);
        Util.validateLang(ctx, lang);
        if (ctx.failed()) return;

        V2Key request = new V2Key("x", "x", region, MarketEndpoint.GetWorldMarketHotList, lang); //String.format("x:x:%s:0", region);
        Future<Buffer> hotlist = CacheManager.getV2Cache(region).get(request);
        hotlist.onSuccess(list -> {
            if (list.toJsonArray().size() == 1) ctx.response().end(list.toJsonArray().getJsonObject(0).encodePrettily());
            else ctx.response().end(list.toJsonArray().encodePrettily());
        }).onFailure(fail -> ctx.fail(500));
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

        V2Key request = new V2Key(mainCategory, subCategory, region, MarketEndpoint.GetWorldMarketList, "x"); //String.format("%s:%s:%s:1", mainCategory, subCategory, region);
        Future<Buffer> res = CacheManager.getV2Cache(region).get(request);
        res.onSuccess(list -> {
            if (list.toJsonArray().isEmpty()) ctx.fail(513);
            else ctx.response().end(list.toJsonArray().encodePrettily());
        }).onFailure(fail -> ctx.fail(500));
    }

    private static void GetWorldMarketSubList(RoutingContext ctx) {
        ctx.response().putHeader("Access-Control-Allow-Origin", "*");
        ctx.response().putHeader("Content-Type", "application/json");

        String region = ctx.request().getParam("region");
        Util.validateRegion(ctx, region);
        if (ctx.failed()) return;

        MultiMap params = ctx.request().params();
        List<String> ids = params.getAll("id");

        String lang = Util.parseLang(ctx.request().getParam("lang"), region);
        Util.validateLang(ctx, lang);
        if (ctx.failed()) return;


        List<Future> requests = new ArrayList<>();      
        ids.forEach(id -> {
            V2Key request = new V2Key(id, "0", region, MarketEndpoint.GetWorldMarketSubList, lang); // String.format("%s:%s:%s:%s", id, 0, region, 2);
            requests.add(CacheManager.getV2Cache(region).get(request));
        });
 
        CompositeFuture.all(requests).onSuccess(cf -> {
            JsonArray res = new JsonArray();
            for (Future<Buffer> item : requests) res.add(item.result().toJsonArray());

            if (res.size() == 1) ctx.response().end(res.getJsonArray(0).encodePrettily());
            else ctx.response().end(res.encodePrettily());
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

        List<Future> searchItems = new ArrayList<>();
        for (String id : ids.split("[,]")) searchItems.add(getSearchItem(new V2Key(id, "x", region, MarketEndpoint.GetWorldMarketSearchList, "x")));
        
        CompositeFuture.all(searchItems).onSuccess(cf -> {
            JsonArray res = new JsonArray();
            for (Future<Buffer> item : searchItems) res.add(item.result().toJsonObject());

            if (res.size() == 1) ctx.response().end(res.getJsonObject(0).encodePrettily());
            else ctx.response().end(res.encodePrettily());
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

        List<Future> requests = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String sid = sids.isEmpty() ? "0" : sids.get(i);
            V2Key request = new V2Key(id, sid, region, requestId, "x"); //String.format("%s:%s:%s:%s", id, sid, region, requestID);
            requests.add(CacheManager.getV2Cache(region).get(request));
        }

        CompositeFuture.all(requests).onSuccess(ar -> {
            JsonArray items = new JsonArray();
            for (Future<Buffer> item : requests) items.add(item.result().toJsonObject());

            if (items.size() == 1) ctx.response().end(items.getJsonObject(0).encodePrettily());
            else ctx.response().end(items.encodePrettily());
        }).onFailure(fail -> ctx.fail(500));
    }

}
