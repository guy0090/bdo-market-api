package io.arsha.api.market;

import io.arsha.api.cache.V1Composite;
import io.arsha.api.common.AppConfig;
import io.arsha.api.market.enums.MarketEndpoint;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

public class Marketplace {
  private static JsonObject regions;
  private static WebClient client;

  /**
  * Initialize and pass <code>Vertx</code> instance.
  *
  * @param vertx the <code>Vertx</code> instance
  * @param conf  the <code>JsonObject</code> config
  * @return      <code>Future</code> with success or fail
  */
  public static Future<Void> init(Vertx vertx, AppConfig conf) {
    Promise<Void> init = Promise.promise();
    regions = conf.getUtil().getJsonObject("regions");
    WebClientOptions options = new WebClientOptions().setSsl(true).setUserAgent("BlackDesert");
    client = WebClient.create(vertx, options);

    if (!regions.isEmpty() && regions != null) {
      init.complete();
    } else {
      init.fail("Failed init for Marketplace");
    }

    return init.future();
  }

  /**
  * Get supported regions.
  *
  * @return <code>JsonObject</code> regions
  */
  public static JsonObject getRegions() {
    return regions;
  }

  /**
  * Send a request to the BDO market.
  *
  * @param request the <code>V1Composite</code> request to send
  * @return        <code>Future&lt;Buffer&gt;</code> with result of
  *                market response or <code>Throwable</code> on fail
  */
  public static Future<Buffer> request(V1Composite request) {
    JsonObject params = parseRequest(request);
    String url = getRegions().getString(params.getString("region"));
    String endpoint = params.getString("endpoint");
    JsonObject body = params.getJsonObject("requestBody");

    Promise<Buffer> response = Promise.promise();
    client.post(443, url, "/Trademarket/" + endpoint)
        .expect(ResponsePredicate.JSON)
        .sendJsonObject(body)
        .onSuccess(res -> response.complete(res.body()))
        .onFailure(response::fail);

    return response.future();
  }

  /**
  * Get request params from request.
  *
  * @param request the <code>V1Composite</code> to process
  * @return        <code>JsonObject</code> with request params
  */
  private static JsonObject parseRequest(V1Composite request) {
    Object arg1 = request.getId();
    Long arg2 = request.getSid();
    String region = request.getRegion();
    MarketEndpoint requestId = request.getRequestId();

    JsonObject requestParams = new JsonObject();
    JsonObject requestBody = new JsonObject();

    switch (requestId) {
      case GetWorldMarketHotList:
        requestBody = null;
        break;
      case GetWorldMarketList:
        requestBody.put("keyType", 0)
            .put("mainCategory", (Long) arg1)
            .put("subCategory", arg2);
        break;
      case GetWorldMarketSubList:
        requestBody.put("keyType", 0)
            .put("mainKey", (Long) arg1);
        break;
      case GetWorldMarketSearchList:
        requestBody.put("searchResult", (String) arg1);
        break;
      case GetBiddingInfoList:
        requestBody.put("keyType", 0)
            .put("mainKey", (Long) arg1)
            .put("subKey", arg2);
        break;
      case GetMarketPriceInfo:
        requestBody.put("keyType", 0)
            .put("mainKey", (Long) arg1)
            .put("subKey", arg2);
        break;
      default:
        break;
    }

    requestParams.put("region", region);
    requestParams.put("requestBody", requestBody);
    requestParams.put("endpoint", requestId.toString());
    return requestParams;
  }
}
