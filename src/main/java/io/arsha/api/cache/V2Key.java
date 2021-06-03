package io.arsha.api.cache;

import io.arsha.api.market.enums.MarketEndpoint;

public final class V2Key {
    /**
     * Create a V2Key from V1Key
     * 
     * @param child the V1Key 
     * @param lang  the language
     */
    public V2Key(final V1Key child, final String lang) {
        this.id = child.getId();
        this.sid = child.getSid();
        this.region = child.getRegion();
        this.requestId = child.getRequestId();
        this.lang = lang;
    }

    /**
     * the composite key for V2 requests
     * 
     * @param id        the id of the item
     * @param sid       the sid of the item
     * @param region    the game region
     * @param requestId the market endpoint
     * @param lang      the language
     */
    public V2Key(final String id, final String sid, final String region, final MarketEndpoint requestId, final String lang) {
        this.id = id;
        this.sid = sid;
        this.region = region.toLowerCase();
        this.requestId = requestId;
        this.lang = lang;
    }

    private String id;
    private String sid;
    private String region;
    private MarketEndpoint requestId;
    private String lang;

    public String getId() {
        return this.id;
    }

    public String getSid() {
        return this.sid;
    }

    public String getRegion() {
        return this.region;
    }

    public MarketEndpoint getRequestId() {
        return this.requestId;
    }

    public String getLang() {
        return this.lang;
    }

    /**
     * Get a V1Key from existing V2Key
     * 
     * @return V1Key
     */
    public V1Key getChild() {
        return new V1Key(id, sid, region, requestId);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        V2Key key = (V2Key) other;
        if (!id.equals(key.id)) return false;
        if (!sid.equals(key.sid)) return false;
        if (!region.equals(key.region)) return false;
        if (!requestId.equals(key.requestId)) return false;
        if (!lang.equals(key.lang)) return false;
        return id.equals(key.id);  
    }

    @Override
    public int hashCode() {
        int hashCode = id.hashCode();
        hashCode = 31 * hashCode
         + sid.hashCode()
         + region.hashCode()
         + requestId.hashCode()
         + lang.hashCode();
        return hashCode;
    }
}
