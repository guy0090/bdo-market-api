package io.arsha.api.routes.v1;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.cache.V1Composite;
import io.arsha.api.cache.V2Composite;
import io.arsha.api.market.Marketplace;
import io.arsha.api.market.enums.MarketEndpoint;
import io.arsha.api.util.Util;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class V1 {
  private static Logger logger = LoggerFactory.getLogger(V1.class);

  /**
   *  Register V1 operations.
   *
   * @param api the <code>RouterBuilder</code> from OpenAPI specification
   */
  public static void registerOperations(RouterBuilder api) {
    api.operation("v1WaitList")
      .handler(V1::getWorldMarketWaitList)
      .failureHandler(Util::handleError);
    api.operation("v1PostWaitList")
      .handler(V1::getWorldMarketWaitList)
      .failureHandler(Util::handleError);

    api.operation("v1HotList")
      .handler(V1::getWorldMarketHotList)
      .failureHandler(Util::handleError);
    api.operation("v1PostHotList")
      .handler(V1::getWorldMarketHotList)
      .failureHandler(Util::handleError);

    api.operation("v1MarketList")
      .handler(V1::getWorldMarketList)
      .failureHandler(Util::handleError);
    api.operation("v1PostMarketList")
      .handler(V1::getWorldMarketList)
      .failureHandler(Util::handleError);

    api.operation("v1MarketSubList")
      .handler(V1::getWorldMarketSubList)
      .failureHandler(Util::handleError);
    api.operation("v1PostMarketSubList")
      .handler(V1::getWorldMarketSubList)
      .failureHandler(Util::handleError);

    api.operation("v1MarketSearchList")
      .handler(V1::getWorldMarketSearchList)
      .failureHandler(Util::handleError);
    api.operation("v1PostMarketSearchList")
      .handler(V1::getWorldMarketSearchList)
      .failureHandler(Util::handleError);

    api.operation("v1MarketBiddingList")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);
    api.operation("v1PostMarketBiddingList")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);

    api.operation("v1MarketPriceInfo")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);
    api.operation("v1PostMarketPriceInfo")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);


    api.operation("v1Price")
      .handler(V1::price)
      .failureHandler(Util::handleError);
    api.operation("v1PostPrice")
      .handler(V1::price)
      .failureHandler(Util::handleError);

    // Legacy Aliases
    api.operation("v1AliasItem")
      .handler(V1::getWorldMarketSubList)
      .failureHandler(Util::handleError);
    api.operation("v1PostAliasItem")
      .handler(V1::getWorldMarketSubList)
      .failureHandler(Util::handleError);

    api.operation("v1AliasOrders")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);
    api.operation("v1PostAliasOrders")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetBiddingInfoList))
      .failureHandler(Util::handleError);

    api.operation("v1AliasHistory")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);
    api.operation("v1PostAliasHistory")
      .handler(ctx -> getBiddingOrPriceInfo(ctx, MarketEndpoint.GetMarketPriceInfo))
      .failureHandler(Util::handleError);
  }

  /**
   * GetWorldMarketWaitList
   * Due to frequent wait list changes, this endpoint is not cached.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  private static void getWorldMarketWaitList(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    String region = ctx.request().getParam("region");
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    Future<Buffer> marketResponse = Marketplace.request(
        new V1Composite(0L, 0L, region, MarketEndpoint.GetWorldMarketWaitList)
    );

    marketResponse.onSuccess(waitList -> {
      logger.info(Util.formatLog(ctx.request()));
      ctx.response().end(waitList.toJsonObject().encodePrettily());
    }).onFailure(fail ->
        ctx.fail(500)
    );
  }

  /**
   * GetWorldMarketHotList.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  private static void getWorldMarketHotList(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    String region = ctx.request().getParam("region");
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    Future<Buffer> cacheResponse = CacheManager.getV1Cache(region)
        .get(new V1Composite(0L, 0L, region, MarketEndpoint.GetWorldMarketHotList));

    cacheResponse.onSuccess(hotList -> {
      logger.info(Util.formatLog(ctx.request()));
      ctx.response().end(hotList.toJsonObject().encodePrettily());
    }).onFailure(fail ->
        ctx.fail(500)
    );
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

    V1Composite request = new V1Composite(mainCategory, subCategory,
        region, MarketEndpoint.GetWorldMarketList);
    Future<Buffer> cacheResponse = CacheManager.getV1Cache(region).get(request);

    cacheResponse.onSuccess(marketList -> {
      try {
        logger.info(Util.formatLog(ctx.request()));
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

    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(region);
    JsonArray param = new JsonArray();
    if (ctx.request().method() == HttpMethod.POST) {
      param = params.headerParameter("id").getJsonArray();
    } else {
      param = params.queryParameter("id").getJsonArray();
    }
    List<Long> ids = param.stream()
        .map(Long.class::cast)
        .collect(Collectors.toList());

    List<Future> buffers = new ArrayList<>();
    ids.forEach(id -> {
      V1Composite request = new V1Composite(id, 0L, region, MarketEndpoint.GetWorldMarketSubList);
      Future cacheResponse = cache.get(request);
      buffers.add(cacheResponse);
    });

    CompositeFuture.all(buffers).onSuccess(ar -> {
      JsonArray items = new JsonArray();
      for (Future<Buffer> buffer : buffers) {
        items.add(buffer.result().toJsonObject());
      }

      if (items.size() == 1) {
        ctx.response().end(items.getJsonObject(0).encodePrettily());
      } else {
        ctx.response().end(items.encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * GetWorldMarketSearchList.
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

    String ids = StringUtils.join(param.getList().toArray(), ",");
    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(region);
    V1Composite request = new V1Composite(ids, 0L,
        region, MarketEndpoint.GetWorldMarketSearchList);

    cache.get(request).onSuccess(res -> {
      logger.info(Util.formatLog(ctx.request()));
      ctx.response().end(res.toJsonObject().encodePrettily());
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * GetBiddingInfoList and GetMarketPriceInfo.
   * @param ctx       the <code>RoutingContext</code>
   * @param requestId the <code>MarketEndpoint</code> request identifier
   */
  private static void getBiddingOrPriceInfo(RoutingContext ctx, MarketEndpoint requestId) {
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
      sidParam = (sid == null ? new JsonArray().add(0L) : sid.getJsonArray());
    } else {
      idParam = params.queryParameter("id").getJsonArray();
      RequestParameter sid = params.queryParameter("sid");
      sidParam = (sid == null ? new JsonArray().add(0L) : sid.getJsonArray());
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

    Cache<V1Composite, Future<Buffer>> cache = CacheManager.getV1Cache(region);
    List<Future> futures = new ArrayList<>();

    for (int i = 0; i < ids.size(); i++) {
      Long id = ids.get(i);
      Long sid = (sids.isEmpty() ? 0L : sids.get(i));

      V1Composite request = new V1Composite(id, sid, region, requestId);
      futures.add(cache.get(request));
    }

    CompositeFuture.all(futures).onSuccess(ar -> {
      JsonArray items = new JsonArray();
      for (Future<Buffer> buffer : futures) {
        items.add(buffer.result().toJsonObject());
      }

      if (items.size() == 1) {
        ctx.response().end(items.getJsonObject(0).encodePrettily());
      } else {
        ctx.response().end(items.encodePrettily());
      }
      logger.info(Util.formatLog(ctx.request()));
    }).onFailure(fail -> ctx.fail(500));
  }

  /**
   * Price.
   * @param ctx the <code>RoutingContext</code>
   */
  private static void price(RoutingContext ctx) {
    ctx.response().putHeader("Access-Control-Allow-Origin", "*");
    ctx.response().putHeader("Content-Type", "application/json");

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String region = params.pathParameter("region").getString();
    Util.validateRegion(ctx, region);
    if (ctx.failed()) {
      return;
    }

    Long id;
    Long sid;
    RequestParameter langParam = null;
    if (ctx.request().method() == HttpMethod.POST) {
      id = params.headerParameter("id").getLong();
      RequestParameter sidParam = params.headerParameter("sid");
      sid = (sidParam == null ? 0L : sidParam.getLong());
      langParam = params.headerParameter("lang");
    } else {
      id = params.queryParameter("id").getLong();
      RequestParameter sidParam = params.queryParameter("sid");
      sid = (sidParam == null ? 0L : sidParam.getLong());
      langParam = params.queryParameter("lang");
    }

    String lang = Util.parseLang(langParam, region);
    Util.validateLang(ctx, lang);
    if (ctx.failed()) {
      return;
    }

    V2Composite request = new V2Composite(id, sid,
        region, MarketEndpoint.GetWorldMarketSubList, lang);
    Future<Buffer> sublistItem = CacheManager.getV2Cache(region).get(request);
    sublistItem.onSuccess(res -> {
      JsonArray result = res.toJsonArray();
      if (result.size() == 1 && sid != 0)  {
        ctx.fail(457);
        return;
      }

      if (result.getJsonObject(0).getString("name") == null) {
        ctx.fail(458);
        return;
      }

      Optional<JsonObject> filter = result.stream()
          .filter(JsonObject.class::isInstance)
          .map(JsonObject.class::cast)
          .filter(obj -> obj.getLong("sid") == Long.valueOf(sid))
          .findFirst();

      if (filter.isPresent()) {
        JsonObject price = filter.get();
        ctx.response().end(new JsonObject()
            .put("name", price.getString("name"))
            .put("id", price.getInteger("id"))
            .put("sid", price.getInteger("sid"))
            .put("basePrice", price.getLong("basePrice"))
            .put("icon", price.getString("icon")).encodePrettily());
        logger.info(Util.formatLog(ctx.request()));
      } else {
        ctx.fail(458);
      }
    });
  }

}
