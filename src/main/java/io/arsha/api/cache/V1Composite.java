package io.arsha.api.cache;

import io.arsha.api.market.enums.MarketEndpoint;

public class V1Composite {

  /**
  * the composite key for V1 requests.
  *
  * @param id        the item id
  * @param sid       the item sub id
  * @param region    the game region
  * @param requestId the request endpoint
  */
  public V1Composite(final Object id, final Long sid, final String region, final MarketEndpoint requestId) {
    this.id = id;
    this.sid = sid;
    this.region = region.toLowerCase();
    this.requestId = requestId;
  }

  private Object id;
  private Long sid;
  private String region;
  private MarketEndpoint requestId;

  public Object getId() {
    return this.id;
  }

  public Long getSid() {
    return this.sid;
  }

  public String getRegion() {
    return this.region;
  }

  public MarketEndpoint getRequestId() {
    return this.requestId;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    V1Composite key = (V1Composite) other;
    if (!id.equals(key.id)) return false;
    if (!sid.equals(key.sid)) return false;
    if (!region.equals(key.region)) return false;
    if (!requestId.equals(key.requestId)) return false;
    return id.equals(key.id);
  }

  @Override
  public int hashCode() {
    int hashCode = id.hashCode();
    hashCode = 31 * hashCode
        + sid.hashCode()
        + region.hashCode()
        + requestId.hashCode();
    return hashCode;
  }
}
