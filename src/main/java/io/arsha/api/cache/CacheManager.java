package io.arsha.api.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.io.CacheLoader;

import io.arsha.api.common.AppConfig;
import io.arsha.api.market.Marketplace;
import io.arsha.api.routes.v2.V2;
import io.arsha.api.util.mongodb.Mongo;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class CacheManager {
    private static Map<String, Cache<V1Composite, Future<Buffer>>> v1Cache = new HashMap<>();
    private static Map<String, Cache<V2Composite, Future<Buffer>>> v2Cache = new HashMap<>();
    private static Cache<UtilComposite, Future<JsonObject>> itemDBCache = null;
    private static Cache<UtilComposite, Future<List<JsonObject>>> fullDBCache = null;
    private static JsonObject config = new JsonObject();

    /**
     * Initialize config and create caches
     * 
     * @param conf <code>JsonObject</code> the config
     * @return     <code>Future</code> with success or fail
     */
    public static Future<Void> init(AppConfig conf) {
        Promise<Void> init = Promise.promise();
        config = conf.getCache();
        CompositeFuture.all(
            createV1Cache(config.getInteger("v1Expiry")),
            createV2Cache(config.getInteger("v2Expiry")),
            createDBCache(config.getInteger("dbExpiry")),
            createFullDBCache(config.getInteger("fullDBExpiry"))
        ).onSuccess(done -> {
            init.complete();
        }).onFailure(fail -> init.fail(fail));

        return init.future();
    }

    public static Future<Void> createV1Cache(Integer expire) {
        return Future.future(cache -> {
            JsonObject regions = Marketplace.getRegions();
            regions.forEach(entry -> {
            String region = entry.getKey();

            v1Cache.put(region.toLowerCase(), new Cache2kBuilder<V1Composite, Future<Buffer>>() {
            }.name(region.toUpperCase() + "_V1").expireAfterWrite(expire, TimeUnit.MINUTES)
                .refreshAhead(true).entryCapacity(40000)
                .loader(new CacheLoader<V1Composite, Future<Buffer>>() {
                    @Override
                    public Future<Buffer> load(V1Composite key) {
                        return Marketplace.request(key);
                    }
                }).build());
            });
            cache.complete();
        });
    }

    public static Future<Void> createV2Cache(Integer expire) {
        return Future.future(cache -> {
            JsonObject regions = Marketplace.getRegions();
            regions.forEach(entry -> {
            String region = entry.getKey();

            v2Cache.put(region.toLowerCase(), new Cache2kBuilder<V2Composite, Future<Buffer>>() {
            }.name(region.toUpperCase() + "_V2").expireAfterWrite(expire, TimeUnit.MINUTES)
                .refreshAhead(true).entryCapacity(40000)
                .loader(new CacheLoader<V2Composite, Future<Buffer>>() {
                    @Override
                    public Future<Buffer> load(V2Composite key) {
                        Future<Buffer> cached = Future.future(null);
                        switch (key.getRequestId()) {
                            case GetWorldMarketHotList:
                                cached = V2.getHotList(key);
                                break;
                            case GetWorldMarketList:
                                cached = V2.getMarketList(key);
                                break;
                            case GetWorldMarketSubList:
                                cached = V2.getSubListItem(key);
                                break;
                            case GetWorldMarketSearchList:
                                cached = V2.getSearchItem(key);
                                break;
                            case GetBiddingInfoList:
                                cached = V2.getBiddingList(key); 
                                break;
                            case GetMarketPriceInfo:
                                cached = V2.getPriceInfo(key); 
                                break;
                        }
                        return cached;
                    }
                }).build());
            });
            cache.complete();
        });
    }

    public static Future<Void> createDBCache(Integer expire) {
        return Future.future(cache -> {
            itemDBCache = new Cache2kBuilder<UtilComposite, Future<JsonObject>>() {
            }.name("ITEM_DB").expireAfterWrite(expire, TimeUnit.MINUTES)
                .refreshAhead(true).entryCapacity(40000)
                .loader(new CacheLoader<UtilComposite, Future<JsonObject>>() {
                    @Override
                    public Future<JsonObject> load(UtilComposite key) {
                        if (key.getCollection().contains("recipe")) return Mongo.getRecipe(key.getCollection(), key.getQuery());
                        else return Mongo.getItem(key.getCollection(), key.getQuery());
                    }
                }).build();
            cache.complete();
        });
    }

    public static Future<Void> createFullDBCache(Integer expire) {
        return Future.future(cache -> {
            fullDBCache = new Cache2kBuilder<UtilComposite, Future<List<JsonObject>>>() {
            }.name("FULL_ITEM_DB").expireAfterWrite(expire, TimeUnit.MINUTES).entryCapacity(20)
                .loader(new CacheLoader<UtilComposite, Future<List<JsonObject>>>() {
                    @Override
                    public Future<List<JsonObject>> load(UtilComposite key) {
                        if (key.getCollection().contains("recipe")) return Mongo.getRecipeClient().find(key.getCollection(), key.getQuery());
                        else return Mongo.getItemClient().find(key.getCollection(), key.getQuery());
                    }
                }).build();
            cache.complete();
        });
    }

    public static Cache<V1Composite, Future<Buffer>> getV1Cache(String region) {
        if (v1Cache.isEmpty()) createV1Cache(config.getInteger("v1Expiry"));
        return v1Cache.get(region.toLowerCase());
    }

    public static Cache<V2Composite, Future<Buffer>> getV2Cache(String region) {
        if (v2Cache.isEmpty()) createV2Cache(config.getInteger("v2Expiry"));
        return v2Cache.get(region.toLowerCase());
    }

    public static Cache<UtilComposite, Future<JsonObject>> getDBCache() {
        if (itemDBCache == null) createDBCache(config.getInteger("dbExpiry"));
        return itemDBCache;
    }

    public static Cache<UtilComposite, Future<List<JsonObject>>> getFullDBCache() {
        if (fullDBCache == null) createFullDBCache(config.getInteger("fullDBExpiry"));
        return fullDBCache;
    }

}
