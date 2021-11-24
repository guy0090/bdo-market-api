package io.arsha.api.market.items;

import io.vertx.core.json.JsonObject;

public class WaitListItem {

  /**
   * Object for GetWorldMarketWaitList result.
   *
   * @param itemString the <code>String[]</code> split resultMsg
   *                   from central market response
   */
  public WaitListItem(String[] itemString) {
    this.id = Long.valueOf(itemString[0]);
    this.sid = Long.valueOf(itemString[1]);
    this.price = Long.valueOf(itemString[2]);
    this.available = Long.valueOf(itemString[3]);
    this.name = null;
  }

  private String name;
  private Long id;
  private Long sid;
  private Long price;
  private Long available;

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

  public Long getPrice() {
    return this.price;
  }

  public void setPrice(Long price) {
    this.price = price;
  }

  public Long getAvailable() {
    return this.available;
  }

  public void setAvailable(Long available) {
    this.available = available;
  }

  /**
  * Returns <code>WaitListItem</code> as <code>JsonObject</code>.
  *
  * @return the <code>JsonObject</code>
  */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("name", name)
        .put("id", id)
        .put("subId", sid)
        .put("price", price)
        .put("liveAt", available);
    //  .put("availableIn", Util.getAvailableTime(available));
    return json;
  }

}
