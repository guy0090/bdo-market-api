package io.arsha.api.market.items;

import io.vertx.core.json.JsonObject;

public class Order {

  /**
   * Object for GetBiddingInfoList result.
   * @param orderString the <code>String[]</code> split resultMsg
   *                    from central market response
   */
  public Order(String[] orderString) {
    this.price = Long.valueOf(orderString[0]);
    this.buyers = Long.valueOf(orderString[1]);
    this.sellers = Long.valueOf(orderString[2]);
  }

  private Long price;
  private Long buyers;
  private Long sellers;

  public Long getPrice() {
    return this.price;
  }

  public void setPrice(Long price) {
    this.price = price;
  }

  public Long getBuyers() {
    return this.buyers;
  }

  public void setBuyers(Long buyers) {
    this.buyers = buyers;
  }

  public Long getSellers() {
    return this.sellers;
  }

  public void setSellers(Long sellers) {
    this.sellers = sellers;
  }

  /**
  * Returns <code>Order</code> as <code>JsonObject</code>.
  *
  * @return the <code>JsonObject</code>
  */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("price", price)
        .put("buyers", buyers)
        .put("sellers", sellers);
    return json;
  }

}
