package io.arsha.api.market.items;

import io.vertx.core.json.JsonObject;

public class Item {

  public Item(Object id, Long sid) {
    this.id = (Long) id;
    this.sid = sid;
  }

  /**
  * Object for GetWorldMarketSubList result.
  * @param itemString the <code>String[]</code> split resultMsg
  *                   from central market response
  */
  public Item(String[] itemString) {
    this.id = Long.valueOf(itemString[0]);
    this.sid = Long.valueOf(itemString[1]);
    this.minEnhance = itemString[1];
    this.maxEnhance = itemString[2];
    this.basePrice = Long.valueOf(itemString[3]);
    this.currentStock = Long.valueOf(itemString[4]);
    this.totalTrades = Long.valueOf(itemString[5]);
    this.priceMin = Long.valueOf(itemString[6]);
    this.priceMax = Long.valueOf(itemString[7]);
    this.lastSoldPrice = Long.valueOf(itemString[8]);
    this.lastSoldTime = Long.valueOf(itemString[9]);
    this.name = null;
    this.icon = null;
  }

  private String name;
  private Long id;
  private Long sid;
  private String minEnhance;
  private String maxEnhance;
  private Long basePrice;
  private Long currentStock;
  private Long totalTrades;
  private Long priceMin;
  private Long priceMax;
  private Long lastSoldPrice;
  private Long lastSoldTime;
  private String icon;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSid() {
    return this.sid;
  }

  public void setSid(Long sid) {
    this.sid = sid;
  }

  public String getMinEnhance() {
    return this.minEnhance;
  }

  public void setMinEnhance(String minEnhance) {
    this.minEnhance = minEnhance;
  }

  public String getMaxEnhance() {
    return this.maxEnhance;
  }

  public void setMaxEnhance(String maxEnhance) {
    this.maxEnhance = maxEnhance;
  }

  public Long getBasePrice() {
    return this.basePrice;
  }

  public void setBasePrice(Long basePrice) {
    this.basePrice = basePrice;
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

  public Long getPriceMin() {
    return this.priceMin;
  }

  public void setPriceMin(Long priceMin) {
    this.priceMin = priceMin;
  }

  public Long getPriceMax() {
    return this.priceMax;
  }

  public void setPriceMax(Long priceMax) {
    this.priceMax = priceMax;
  }

  public Long getLastSoldPrice() {
    return this.lastSoldPrice;
  }

  public void setLastSoldPrice(Long lastSoldPrice) {
    this.lastSoldPrice = lastSoldPrice;
  }

  public Long getLastSoldTime() {
    return this.lastSoldTime;
  }

  public void setLastSoldTime(Long lastSoldTime) {
    this.lastSoldTime = lastSoldTime;
  }

  public String getIcon() {
    return this.icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  /**
  * Returns <code>Item</code> as <code>JsonObject</code>.
  *
  * @return the <code>JsonObject</code>
  */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("name", name)
        .put("icon", icon)
        .put("id", id)
        .put("sid", sid)
        .put("minEnhance", minEnhance)
        .put("maxEnhance", maxEnhance)
        .put("basePrice", basePrice)
        .put("currentStock", currentStock)
        .put("totalTrades", totalTrades)
        .put("priceMin", priceMin)
        .put("priceMax", priceMax)
        .put("lastSoldPrice", lastSoldPrice)
        .put("lastSoldTime", lastSoldTime);
    return json;
  }
}
