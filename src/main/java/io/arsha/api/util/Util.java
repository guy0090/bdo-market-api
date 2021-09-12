package io.arsha.api.util;

import io.arsha.api.common.AppConfig;
import io.arsha.api.market.Marketplace;
import io.arsha.api.market.items.History;
import io.arsha.api.market.items.Item;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

public class Util {
  private static Logger logger = LoggerFactory.getLogger(Util.class);
  private static Vertx vertx = null;
  private static JsonObject config = new JsonObject();

  /**
   * Initialize and pass <code>Vertx</code> instance.
   *
   * @param vtx the <code>Vertx</code> instance
   * @return <code>Future</code> with success or fail
   */
  public static Future<Void> init(Vertx vtx, AppConfig conf) {
    Promise<Void> init = Promise.promise();
    vertx = vtx;
    config = conf.getUtil();

    if (vertx != null && !config.isEmpty()) {
      init.complete();
    }
    return init.future();
  }

  /**
   * Get all supported languages.
   *
   * @return <code>List&lt;String&gt;</code> with supported languages
   */
  @SuppressWarnings("unchecked")
  public static List<String> getLangs() {
    return config.getJsonArray("languages").getList();
  }

  /**
   * Parsed <codeZoneId>History</codeZoneId> to epoch in given region.
   *
   * @param history the history from a <code>String</code> resultMsg
   * @param region  the region to get time at
   * @return parsed <code>History</code> object
   */
  public static History parseHistory(String history, String region) {
    Map<String, Object> res = new TreeMap<String, Object>();
    History h = new History(history);
    List<String> prices = h.toList();
    for (int i = 0; i < prices.size(); i++) {
      LocalDateTime localizedToday = LocalDateTime.now();
      LocalDateTime localizedN = localizedToday.minusDays(prices.size() - 1 - i);
      res.put(String.valueOf(localizedN.toEpochSecond(ZoneOffset.UTC)), prices.get(i));
    }
    h.setParsed(res);
    return h;
  }

  /**
   * Select an <code>Item</code> by SID.
   *
   * @param response the market response
   * @param sid      the SID to select
   * @return the selected <code>Item</code> or null if not found
   */
  public static Item selectItemBySid(JsonObject response, Integer sid) {
    String[] resultMsg = response.getString("resultMsg").split("[|]");
    if (response.getString("resultMsg").equals("0")) {
      return null;
    }

    if (resultMsg.length > 1) {
      for (String result : resultMsg) {
        String[] details = result.split("[-]");
        if (Integer.valueOf(details[1]) == sid) {
          return new Item(details);
        }
      }
      return null;
    }

    return new Item(resultMsg[0].split("[-]"));
  }

  /**
   * Get the prefix of an item based on min/max enhancement values.
   *
   * <p>Prefix can be any of the following: +1 to +15 & PRI to PEN.
   *
   * @param id         the item id
   * @param minEnhance the minimum enhancement level
   * @param maxEnhance the maximum enhancement level
   * @return the <code>String</code> item prefix
   */
  public static String getItemPrefix(Long id, String minEnhance, String maxEnhance) {
    // Silver Embroidered Goes 0,1,2,3,4,5 instead of 0,PRI,DUO,TRI,TET,PEN
    List<Long> specialCases = Arrays.asList(14025L, 14019L, 14026L, 14028L,
        14020L, 14022L, 14024L,
        14029L, 14021L, 14023L);

    Integer min = Integer.valueOf(minEnhance);
    Integer max = Integer.valueOf(maxEnhance);

    if (min < max && min != 0) {
      return String.format("+%s ", min);
    }

    if (min == max) {
      if (min == 0) {
        return "";
      }
      if (min < 6 && min > 0) {
        switch (min) {
          case 1:
            if (specialCases.contains(id))  {
              return "+1 ";
            }
            return "PRI: ";
          case 2:
            if (specialCases.contains(id)) {
              return "+2 ";
            }
            return "DUO: ";
          case 3:
            if (specialCases.contains(id)) {
              return "+3 ";
            }
            return "TRI: ";
          case 4:
            if (specialCases.contains(id)) {
              return "+4 ";
            }
            return "TET: ";
          case 5:
            if (specialCases.contains(id)) {
              return "+5 ";
            }
            return "PEN: ";
          default:
            break;
        }
      }
      if (min > 15) {
        switch (min) {
          case 16:
            return "PRI: ";
          case 17:
            return "DUO: ";
          case 18:
            return "TRI: ";
          case 19:
            return "TET: ";
          case 20:
            return "PEN: ";
          default:
            break;
        }
      }
      return String.format("+%s ", min);
    }
    return "";
  }

  /**
   * Saves the given buffer to file.
   *
   * @param content the content to save as a <code>Buffer</code>
   * @param file    the path to the file
   * @return        <code>Future&lt;String&gt;</code> with result <code>String</code> or
   *                <code>Throwable</code> if fail
   */
  public static Future<String> save(Buffer content, String file) {
    Promise<String> promise = Promise.promise();
    vertx.fileSystem().mkdirs(file, result -> {
      if (result.succeeded()) {
        vertx.fileSystem().writeFile(file, content, write -> {
          if (write.succeeded()) {
            promise.complete("Wrote file: " + file);
          } else {
            promise.fail(result.cause());
          }
        });
      } else {
        promise.fail(result.cause());
      }
    });

    return promise.future();
  }

  /**
   * Read a file from given path.
   *
   * @param path the path to the file
   * @return <code>Future&lt;Buffer&gt;</code> with <code>Buffer</code> result or
   *         <code>Throwable</code> if fail
   */
  public static Future<Buffer> read(String path) {
    Promise<Buffer> file = Promise.promise();

    vertx.fileSystem().readFile(path, result -> {
      if (result.succeeded()) {
        file.complete(result.result());
      } else {
        file.fail(result.cause());
      }
    });

    return file.future();
  }

  /**
   * Handle errors passed by <code>RoutingContext</code> failure handler.
   *
   * @param ctx the <code>RoutingContext</code>
   */
  public static void handleError(RoutingContext ctx) {
    ctx.response().putHeader("Content-Type", "application/json");

    String id = UUID.randomUUID().toString();
    JsonObject error = new JsonObject();
    Integer code = ctx.statusCode();
    switch (code) {
      case 400: // Bad request
        error.put("error", code).put("message", "Bad request");
        break;
      case 401: // Unauthorized
        error.put("error", code).put("message", "Not authorized");
        break;
      case 404: // Not found
        error.put("error", code).put("message", "Not found");
        break;
      case 405: // Bad method
        error.put("error", code).put("message", "HTTP method not supported");
        break;
      case 452: // Custom no item found
        error.put("error", code).put("message", "Item could not be found");
        break;
      case 453: // Custom invalid region
        error.put("error", code).put("message", "Invalid region");
        break;
      case 454: // Custom unequal id/sid
        error.put("error", code).put("message",
            "Invalid arguments: There must be an SID for every ID given");
        break;
      case 455: // Custom multiple id not allowed
        error.put("error", code).put("message",
            "Invalid arguments: Multiple id params are not supported under this endpoint");
        break;
      case 456: // Invalid language
        error.put("error", code).put("message", "Invalid language provided");
        break;
      case 457: // Custom item doesn't have sub item
        error.put("error", code).put("message", "Item does not have sub item by that sid");
        break;
      case 458: // Custom price item doesn't exist
        error.put("error", code).put("message", "Invalid price item");
        break;
      case 500: // Unhandled internal error
        error.put("error", code).put("message", "Internal server error");
        break;
      case 512: // Custom db error
        error.put("error", code).put("message", "Database is unreachable");
        break;
      case 513: // Custom encountered error decoding json
        error.put("error", code).put("message", "Encountered an error parsing market response");
        break;
      case 514: // Custom bad response from market
        error.put("error", code).put("message", "Web market did not respond");
        break;
      default:
        error.put("error", code).put("message", "Unexpected error occurred");
        logger.error(ctx.failure().getMessage());
        break;
    }
    String query = ctx.request().query() != null ? "?" + ctx.request().query() : "";
    error.put("request", ctx.request().path() + query);
    error.put("request-id", id);

    String err = String.format("%s: %s?%s | %s | %s",
        ctx.request().method().name(),
        ctx.request().path(),
        ctx.request().query(),
        error.getString("message"),
        id
    );

    logger.error(err);
    ctx.response().setStatusCode(error.getInteger("error")).end(error.encodePrettily());
  }

  /**
   * Check if the region is valid.
   *
   * @param ctx    the <code>RoutingContext</code>
   * @param region the region to validate
   */
  public static void validateRegion(RoutingContext ctx, String region) {
    JsonObject regions = Marketplace.getRegions();

    if (regions.containsKey(region.toLowerCase())) {
      return;
    }
    ctx.fail(453);
  }

  /**
   * Parse the language.
   *
   * <p>If the region is <code>kr</code> and the locale is <code>kr</code> returns
   * <code>kr</code> otherwise if region is null return <code>en</code>
   *
   * @param lang the language to parse
   * @param region the region to use to parse
   * @return the parsed locale as <code>String</code>
   */
  public static String parseLang(RequestParameter lang, String region) {
    if (lang == null) {
      if (region.equalsIgnoreCase("kr")) {
        return "kr";
      } else {
        return "en";
      }
    } else if (region.equalsIgnoreCase("kr") && lang.getString().equalsIgnoreCase("kr")) {
      return "kr";
    } else {
      return lang.getString();
    }
  }

  /**
   * Check if the language is valid.
   *
   * @param ctx  the <code>RoutingContext</code>
   * @param lang the language
   */
  public static void validateLang(RoutingContext ctx, String lang) {
    List<String> langs = getLangs();

    if (langs.contains(lang.toLowerCase())) {
      return;
    }
    ctx.fail(456);
  }

  /**
   * Formats <code>HttpServerRequest</code> into log.
   *
   * @param request The <code>HttpServerRequest</code>
   * @return
   */
  public static String formatLog(HttpServerRequest request) {
    List<String> validHeaders = Arrays.asList(
        "id", "sid", "ids", "mainCategory", "subCategory", "lang");
    String method = request.method().name();
    String path = request.path();
    String query = (request.query() == null ? "null" : request.query());
    MultiMap headers = request.headers();
    List<String> params = new ArrayList<>();

    if (method.equalsIgnoreCase("GET")) {
      params = Arrays.asList(query.split("[&]"));
    } else {
      for (Entry<String, String> entry : headers) {
        String key = entry.getKey();
        if (!validHeaders.contains(key)) {
          continue;
        }

        String val = entry.getValue();
        params.add(String.format("%s=%s", key, val));
      }
      if (params.size() == 0) {
        params.add("null");
      }
    }

    String[] parts = path.split("[/]");

    String version = parts[1];
    if (version.equalsIgnoreCase("util")) {
      return new JsonObject()
        .put("METHOD", method)
        .put("API_VERSION", "UTIL")
        .put("ENDPOINT", (parts.length <= 3 ? parts[2] : parts[2] + "/" + parts[3]))
        .put("QUERY", String.join(";", params))
        .encode();
    }

    return new JsonObject()
      .put("METHOD", method)
      .put("API_VERSION", version.toUpperCase())
      .put("REGION", path.split("[/]")[2].toUpperCase())
      .put("ENDPOINT", path.split("[/]")[3])
      .put("QUERY", String.join(";", params))
      .encode();
  }
}
