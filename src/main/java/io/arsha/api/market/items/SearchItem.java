package io.arsha.api.market.items;

import io.vertx.core.json.JsonObject;

public class SearchItem {
  public SearchItem(Object id) {
    this.id = Long.valueOf((String) id);
  }

  /**
   * Object for GetMarketSearchList result.
   * @param detailString the <code>String[]</code> split resultMsg
   *                     from central market response
   */
  public SearchItem(String[] detailString) {
    this.id = Long.valueOf(detailString[0]);
    this.currentStock = Long.valueOf(detailString[1]);
    this.totalTrades = Long.valueOf(detailString[3]);
    this.basePrice = Long.valueOf(detailString[2]);
  }

  private Long id;
  private Long currentStock;
  private Long totalTrades;
  private Long basePrice;

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCurrentStock() {
    return this.currentStock;
  }

  public void setCurrentStock(Long currentStock) {
    this.currentStock = currentStock;
  }

  public Long getTotalTrades() {
    return this.totalTrades;
  }

  public void setTotalTrades(Long totalTrades) {
    this.totalTrades = totalTrades;
  }

  public Long getBasePrice() {
    return this.basePrice;
  }

  public void setBasePrice(Long basePrice) {
    this.basePrice = basePrice;
  }

  /**
  * Returns <code>SearchItem</code> as <code>JsonObject</code>.
  *
  * @return the <code>JsonObject</code>
  */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("currentStock", currentStock)
        .put("totalTrades", totalTrades)
        .put("basePrice", basePrice);
    return json;
  }
}
