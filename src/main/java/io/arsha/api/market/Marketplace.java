package io.arsha.api.market;

import io.arsha.api.cache.V1Key;
import io.arsha.api.common.AppConfig;
import io.arsha.api.market.enums.MarketEndpoint;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

public class Marketplace {
    private static JsonObject regions; 
    private static HttpClient client; 
    private static HttpClientOptions options = new HttpClientOptions().setSsl(true);

    /**
     * Initialize and pass <code>Vertx</code> instance
     * 
     * @param vertx the <code>Vertx</code> instance
     * @param conf  the <JsonObject> config
     * @return      <code>Future</code> with success or fail
     */
    public static Future<Void> init(Vertx vertx, AppConfig conf) {
        Promise<Void> init = Promise.promise();
        regions = conf.getUtil().getJsonObject("regions");
        client = vertx.createHttpClient(options);
        
        if (!regions.isEmpty() && regions != null) init.complete();
        else init.fail("Failed init for Marketplace");
        return init.future();
    }

    /**
     * Get supported regions
     * 
     * @return <code>JsonObject</code> regions
     */
    public static JsonObject getRegions() {
        return regions;
    }

    /**
     * Send a request to the BDO market
     * 
     * @param request the request to send
     * @return        <code>Future&lt;Buffer&gt;</code> with result of 
     *                market response or <code>Throwable</code> on fail
     */
    public static Future<Buffer> request(V1Key request) {
        JsonObject params = parseRequest(request);
        String url = getRegions().getString(params.getString("region"));
        String endpoint = params.getString("endpoint");
        String body = params.getString("requestBody");

        Promise<Buffer> response = Promise.promise();
        client.request(HttpMethod.POST, 443, url, "/Trademarket/"+endpoint)
        .compose(req -> 
            req.putHeader("Content-Type", "application/json")
            .putHeader("User-Agent", "BlackDesert")
            .send(body)
            .onSuccess(res -> res.body().onSuccess(response::complete))
            .onFailure(response::fail)
        );

        return response.future();
    }

    /**
     * Get request params from request
     * 
     * @param request the V1Key to process
     * @return        JsonObject with request params
     */
    private static JsonObject parseRequest(V1Key request) {
        String arg1 = request.getId();
        String arg2 = request.getSid();
        String region = request.getRegion();
        MarketEndpoint requestId = request.getRequestId();

        JsonObject requestParams = new JsonObject();
        String requestBody = "";

        switch (requestId) {
            case GetWorldMarketHotList:
                break;  
            case GetWorldMarketList:
                requestBody = String.format("{\"keyType\":0,\"mainCategory\":%s, \"subCategory\": %s}", arg1, arg2);
                break;  
            case GetWorldMarketSubList:
                requestBody = String.format("{\"keyType\":0,\"mainKey\":%s}", arg1);
                break;
            case GetWorldMarketSearchList:
                requestBody = String.format("{\"searchResult\":\"%s\"}", arg1);
                break;
            case GetBiddingInfoList:
                requestBody = String.format("{\"keyType\":0,\"mainKey\":%s,\"subKey\": %s}", arg1, arg2);
                break;  
            case GetMarketPriceInfo:
                requestBody = String.format("{\"keyType\":0,\"mainKey\":%s,\"subKey\": %s}", arg1, arg2);
                break;  
        }

        requestParams.put("region", region);
        requestParams.put("requestBody", requestBody);
        requestParams.put("endpoint", requestId.toString());
        return requestParams;
    }
}
