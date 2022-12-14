package io.arsha.api.market.items;

import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class History {

  /**
   * Object for GetMarketPriceInfo result.
   * @param historyString the <code>String[]</code> split resultMsg
   *                      from central market response
   */
  public History(String historyString) {
    this.prices = historyString.split("[-]");
    this.parsed = null;
  }

  private String[] prices;
  private Map<String, Object> parsed;

  public String[] getPrices() {
    return this.prices;
  }

  public void setPrices(String[] prices) {
    this.prices = prices;
  }

  public Map<String,Object> getParsed() {
    return this.parsed;
  }

  public void setParsed(Map<String,Object> parsed) {
    this.parsed = parsed;
  }

  public List<String> toList() {
    List<String> asList = Arrays.asList(prices);
    return asList;
  }

  /**
  * Returns <code>History</code> as <code>JsonObject</code>.
  *
  * @return the <code>JsonObject</code>
  */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("history", parsed);
    return json;
  }

}
