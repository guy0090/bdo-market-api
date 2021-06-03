package io.arsha.api.market.items;

import io.vertx.core.json.JsonObject;

public class HotListItem {
    public HotListItem(String[] itemString) {
        this.id = Integer.valueOf(itemString[0]);
        this.subId = Integer.valueOf(itemString[1]);
        this.minEnhance = itemString[1];
        this.maxEnhance = itemString[2];
        this.basePrice = Long.valueOf(itemString[3]);
        this.currentStock = Long.valueOf(itemString[4]);
        this.totalTrades = Long.valueOf(itemString[5]);
        this.priceChangeDirection = Integer.valueOf(itemString[6]);
        this.priceChangeValue = Long.valueOf(itemString[7]);
        this.priceMin = Long.valueOf(itemString[8]);
        this.priceMax = Long.valueOf(itemString[9]);
        this.lastSoldPrice = Long.valueOf(itemString[10]);
        this.lastSoldTime = Long.valueOf(itemString[11]);
        this.name = null;
        this.icon = null;
    }

    private String name;
    private int id;
    private int subId;
    private String minEnhance;
    private String maxEnhance;
    private Long basePrice;
    private Long currentStock;
    private Long totalTrades;
    private int priceChangeDirection;
    private Long priceChangeValue;
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

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubId() {
        return this.subId;
    }

    public void setSubId(int subId) {
        this.subId = subId;
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

    public int getPriceChangeDirection() {
        return this.priceChangeDirection;
    }

    public void setPriceChangeDirection(int priceChangeDirection) {
        this.priceChangeDirection = priceChangeDirection;
    }

    public Long getPriceChangeValue() {
        return this.priceChangeValue;
    }

    public void setPriceChangeValue(Long priceChangeValue) {
        this.priceChangeValue = priceChangeValue;
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
     * Returns <code>HotListItem</code> as <code>JsonObject</code>
     * 
     * @return the <code>JsonObject</code>
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("name", name)
            .put("icon", icon)
            .put("id", id)
            .put("subId", subId)
            .put("minEnhance", minEnhance)
            .put("maxEnhance", maxEnhance)
            .put("basePrice", basePrice)
            .put("currentStock", currentStock)
            .put("totalTrades", totalTrades)
            .put("priceChangeDirection", priceChangeDirection)
            .put("priceChangeValue", priceChangeValue)
            .put("priceMin", priceMin)
            .put("priceMax", priceMax)
            .put("lastSoldPrice", lastSoldPrice)
            .put("lastSoldTime", lastSoldTime);
        return json;
    }
}
