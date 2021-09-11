package io.arsha.api.routes.v2;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.UtilComposite;
import io.arsha.api.cache.V1Composite;
import io.arsha.api.cache.V2Composite;
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
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
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
import java.util.List;
import java.util.stream.Collectors;

import org.cache2k.Cache;


@SuppressWarnings({ "rawtypes", "unchecked" })
public class V2 {
  private static Logger logger = LoggerFactory.getLogger(V2.class);

  /**
   * Register V2 operations.
   *
   * @param api the <code>RouterBuilder</code> from OpenAPI specification
   */
  public static void registerOperations(RouterBuilder api) {
    api.operation("v2HotList")
      .handler(V2::getWorldMarketHotList)
      .failureHandler(Util::handleError);
    api.operation("v2PostHotList")
      .handler(V2::getWorldMarketHotList)
      .failureHandler(Util::handleError);

    api.operation("v2MarketList")
      .handler(V2::getWorldMarketList)
      .failureHandler(Util::handleError);
    api.operation("v2PostMarketList")
      .handler(V2::getWorldMarketList)
      .failureHandler(Util::handleError);

    api.operation("v2MarketSubList")
      .handler(V2::getWorldMarketSubList)
      .failureHandler(Util::handleError);
    api.operation("v2PostMarketSubList")
      .handler(V2::getWorldMarketSubList)
      .failureHandler(Util::handleError);

    api.operation("v2MarketSearchList")
      .handler(V2::getWorldMarketSearchList)
      .failureHandler(Util::handleError);
    api.operation("v2PostMarketSearchList")
      .handler(V2::getWorldMarketSearchList)
      .failureHandler(Util::handleError);

    api.operation("v2MarketBiddingList")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);
    api.operation("v2PostMarketBiddingList")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);

    api.operation("v2MarketPriceInfo")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);
    api.operation("v2PostMarketPriceInfo")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);

    // Legacy
    api.operation("v2AliasItem")
      .handler(V2::getWorldMarketSubList)
      .failureHandler(Util::handleError);
    api.operation("v2PostAliasItem")
      .handler(V2::getWorldMarketSubList)
      .failureHandler(Util::handleError);

    api.operation("v2AliasOrders")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);
    api.operation("v2PostAliasOrders")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);

    api.operation("v2AliasHistory")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);
    api.operation("v2PostAliasHistory")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);
  }

  /**
  * Get current hot list.
  *
  * @param request the <code>V2Composite</code> composite key
  * @return        <code>Future&lt;Buffer&gt;</code> of the list to grab
  */
  public static Future<Buffer> getHotList(V2Composite request) {
    Promise<Buffer> response = Promise.promise();
    Future<Buffer> hotlist = CacheManager.getV1Cache(request.getRegion()).get(request.getParent());

    hotlist.onSuccess(list -> {
      List<Future> dbFutures = new ArrayList<>();
      List<HotListItem> hotlistItems = new ArrayList<>();
      JsonObject res = hotlist.result().toJsonObject();
      for (String subItem : res.getString("resultMsg").split("[|]")) {
        HotListItem i = new HotListItem(subItem.split("[-]"));
        hotlistItems.add(i);
        UtilComposite util = new UtilComposite(request.getLang(),
            new JsonObject().put("id", i.getId()));
        dbFutures.add(CacheManager.getDbCache().get(util));
      }

      CompositeFuture.all(dbFutures).onSuccess(cf -> {
        JsonArray items = new JsonArray();
        for (int i = 0; i < hotlistItems.size(); i++) {
          JsonObject dbItem = (JsonObject) dbFutures.get(i).result();
          HotListItem item = hotlistItems.get(i);
          String prefix = Util.getItemPrefix(
              item.getId(),
              item.getMinEnhance(),
              item.getMaxEnhance()
          );

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
  * Get all items in specified (sub)category.
  *
  * @param request the <code>V2Composite</code> list composite key
  * @return        <code>Future&lt;Buffer&gt;</code> of the list to grab
  */
  public static Future<Buffer> getMarketList(V2Composite request) {
    Promise<Buffer> response = Promise.promise();
    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
    Future<Buffer> marketList = cache.get(request.getParent());

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
  * Get information for item and variants (enhancement levels).
  *
  * @param request the <code>V2Composite</code> item composite key
  * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
  */
  public static Future<Buffer> getSubListItem(V2Composite request) {
    Promise<Buffer> response = Promise.promise();
    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
    Future<Buffer> itemFuture = cache.get(request.getParent());
    UtilComposite util = new UtilComposite(request.getLang(),
        new JsonObject().put("id", request.getId()));

    Future<JsonObject> dbFuture = CacheManager.getDbCache().get(util);

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
  * Get item search information.
  *
  * @param request the <code>V2Composite</code> item composite key
  * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
  */
  public static Future<Buffer> getSearchItem(V2Composite request) {
    Promise<Buffer> response = Promise.promise();
    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
    cache.get(request.getParent()).onSuccess(result -> {
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
  * Get item order information.
  *
  * @param request the <code>V2Composite</code> item composite key
  * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
  */
  public static Future<Buffer> getBiddingList(V2Composite request) {
    Promise<Buffer> response = Promise.promise();
    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
    cache.get(request.getParent()).onSuccess(result -> {
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
        for (String detail : details) {
          orders.add(new Order(detail.split("[-]")).toJson());
        }
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
  * Get item price history information.
  *
  * @param request the <code>V2Composite</code> item composite key
  * @return        <code>Future&lt;Buffer&gt;</code> of the item to grab
  */
  public static Future<Buffer> getPriceInfo(V2Composite request) {
    Promise<Buffer> response = Promise.promise();
    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(request.getRegion());
    cache.get(request.getParent()).onSuccess(result -> {
      JsonObject asJson = result.toJsonObject();
      JsonObject history = new JsonObject()
          .put("id", request.getId())
          .put("sid", request.getSid());
      if (asJson.getString("resultMsg").equals("0") || asJson.getInteger("resultCode") == 8) {
        history.put("history", new JsonObject());
      } else {
        History hist = Util.parseHistory(asJson.getString("resultMsg"), request.getRegion());
        history.put("history", hist.toJson().getJsonObject("history"));
      }
      response.complete(history.toBuffer());
    }).onFailure(response::fail);

    return response.future();
  }

  /**
   * GetWorldMarketHotList.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  private static void getWorldMarketHotList(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String region = params.pathParameter("region").getString();
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    RequestParameter langParam = (ctx.request().method() == HttpMethod.POST
        ? params.headerParameter("lang") : params.queryParameter("lang"));
    String lang = Util.parseLang(langParam, region);
    Util.validateLang(ctx, lang);
    if (ctx.failed()) {
      return;
    }

    V2Composite request = new V2Composite(0L, 0L,
        region, MarketEndpoint.GetWorldMarketHotList, lang);
    Future<Buffer> hotlist = CacheManager.getV2Cache(region).get(request);
    hotlist.onSuccess(list -> {
      if (list.toJsonArray().size() == 1) {
        ctx.response().end(list.toJsonArray().getJsonObject(0).encodePrettily());
      } else {
        ctx.response().end(list.toJsonArray().encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * GetWorldMarketList.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  private static void getWorldMarketList(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String region = params.pathParameter("region").getString();
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    Long mainCategory;
    Long subCategory;
    if (ctx.request().method() == HttpMethod.POST) {
      mainCategory = params.headerParameter("mainCategory").getLong();
      subCategory = params.headerParameter("subCategory").getLong();
    } else {
      mainCategory = params.queryParameter("mainCategory").getLong();
      subCategory = params.queryParameter("subCategory").getLong();
    }

    V2Composite request = new V2Composite(mainCategory, subCategory,
        region, MarketEndpoint.GetWorldMarketList, "x");
    Future<Buffer> res = CacheManager.getV2Cache(region).get(request);
    res.onSuccess(list -> {
      if (list.toJsonArray().isEmpty()) {
        ctx.fail(513);
      } else {
        ctx.response().end(list.toJsonArray().encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * GetWorldMarketSubList.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  private static void getWorldMarketSubList(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String region = params.pathParameter("region").getString();
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    JsonArray param = new JsonArray();
    RequestParameter langParam = null;
    if (ctx.request().method() == HttpMethod.POST) {
      param = params.headerParameter("id").getJsonArray();
      langParam = params.headerParameter("lang");
    } else {
      param = params.queryParameter("id").getJsonArray();
      langParam = params.queryParameter("lang");
    }

    List<Long> ids = param.stream()
        .map(Long.class::cast)
        .collect(Collectors.toList());

    String lang = Util.parseLang(langParam, region);
    Util.validateLang(ctx, lang);
    if (ctx.failed()) {
      return;
    }

    List<Future> requests = new ArrayList<>();
    ids.forEach(id -> {
      V2Composite request = new V2Composite(id, 0L,
          region, MarketEndpoint.GetWorldMarketSubList, lang);
      requests.add(CacheManager.getV2Cache(region).get(request));
    });

    CompositeFuture.all(requests).onSuccess(cf -> {
      JsonArray res = new JsonArray();
      for (Future<Buffer> item : requests) {
        res.add(item.result().toJsonArray());
      }

      if (res.size() == 1) {
        ctx.response().end(res.getJsonArray(0).encodePrettily());
      } else {
        ctx.response().end(res.encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * GetWorldMarketSearchList.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  private static void getWorldMarketSearchList(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String region = params.pathParameter("region").getString();
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    JsonArray param = new JsonArray();
    if (ctx.request().method() == HttpMethod.POST) {
      param = params.headerParameter("ids").getJsonArray();
    } else {
      param = params.queryParameter("ids").getJsonArray();
    }
    List<String> ids = param.stream()
        .map(String::valueOf)
        .collect(Collectors.toList());

    List<Future> searchItems = new ArrayList<>();
    ids.forEach(id -> {
      V2Composite request = new V2Composite(id, 0L,
          region, MarketEndpoint.GetWorldMarketSearchList, "x");
      searchItems.add(getSearchItem(request));
    });

    CompositeFuture.all(searchItems).onSuccess(cf -> {
      JsonArray res = new JsonArray();
      for (Future<Buffer> item : searchItems) {
        res.add(item.result().toJsonObject());
      }

      if (res.size() == 1) {
        ctx.response().end(res.getJsonObject(0).encodePrettily());
      } else {
        ctx.response().end(res.encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * GetBiddingInfoList and GetMarketPriceInfo.
   * @param ctx       the <code>RoutingContext</code>
   * @param requestId the <code>MarketEndpoint</code> request identifier
   */
  public static void getBiddingOrPriceInfo(RoutingContext ctx, MarketEndpoint requestId) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String region = params.pathParameter("region").getString();
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    JsonArray idParam = new JsonArray();
    JsonArray sidParam = new JsonArray();
    if (ctx.request().method() == HttpMethod.POST) {
      idParam = params.headerParameter("id").getJsonArray();
      RequestParameter sid = params.headerParameter("sid");
      sidParam = (sid == null ? new JsonArray() : sid.getJsonArray());
    } else {
      idParam = params.queryParameter("id").getJsonArray();
      RequestParameter sid = params.queryParameter("sid");
      sidParam = (sid == null ? new JsonArray() : sid.getJsonArray());
    }
    List<Long> ids = idParam.stream()
        .map(Long.class::cast)
        .collect(Collectors.toList());
    List<Long> sids = sidParam.stream()
        .map(Long.class::cast)
        .collect(Collectors.toList());

    if (sids.size() > 0 && (ids.size() != sids.size())) {
      ctx.fail(454);
      return;
    }

    List<Future> requests = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      Long id = ids.get(i);
      Long sid = sids.isEmpty() ? 0L : sids.get(i);
      V2Composite request = new V2Composite(id, sid, region, requestId, "x");
      requests.add(CacheManager.getV2Cache(region).get(request));
    }

    CompositeFuture.all(requests).onSuccess(ar -> {
      JsonArray items = new JsonArray();
      for (Future<Buffer> item : requests) {
        items.add(item.result().toJsonObject());
      }

      if (items.size() == 1) {
        ctx.response().end(items.getJsonObject(0).encodePrettily());
      } else {
        ctx.response().end(items.encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

}
