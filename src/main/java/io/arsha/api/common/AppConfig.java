package io.arsha.api.common;

import io.vertx.core.json.JsonObject;

public class AppConfig {

  /**
   * The app configuration.
   *
   * @param json the Json <code>String</code> source
   */
  public AppConfig(String json) {
    JsonObject config = new JsonObject(json);

    this.debug = config.getBoolean("debug");
    this.docs = config.getString("docs");
    this.app = config.getJsonObject("app");
    this.metrics = config.getJsonObject("metrics");
    this.util = config.getJsonObject("util");
    this.cache = config.getJsonObject("cache");
  }

  private Boolean debug;
  private String docs;
  private JsonObject app;
  private JsonObject metrics;
  private JsonObject util;
  private JsonObject cache;

  public Boolean isDebug() {
    return this.debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public String getDocs() {
    return this.docs;
  }

  public void setDocs(String docs) {
    this.docs = docs;
  }

  public JsonObject getApp() {
    return this.app;
  }

  public void setApp(JsonObject app) {
    this.app = app;
  }

  public JsonObject getMetrics() {
    return this.metrics;
  }

  public void setMetrics(JsonObject metrics) {
    this.metrics = metrics;
  }

  public JsonObject getUtil() {
    return this.util;
  }

  public void setUtil(JsonObject util) {
    this.util = util;
  }

  public JsonObject getCache() {
    return this.cache;
  }

  public void setCache(JsonObject cache) {
    this.cache = cache;
  }

}
