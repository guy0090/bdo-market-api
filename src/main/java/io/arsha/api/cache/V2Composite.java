package io.arsha.api.cache;

import io.arsha.api.market.enums.MarketEndpoint;

public class V2Composite extends V1Composite {
    /**
     * Create a <code>V2Composite</code> from <code>V1Composite</code>
     * 
     * @param child the <code>V1Composite</code> 
     * @param lang  the language
     */
    public V2Composite(final V1Composite child, final String lang) {
        super(child.getId(), child.getSid(), child.getRegion(), child.getRequestId());
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
    public V2Composite(final String id, final String sid, final String region, final MarketEndpoint requestId, final String lang) {
        super(id, sid, region.toLowerCase(), requestId);
        this.lang = lang;
    }

    private String lang;

    public String getLang() {
        return this.lang;
    }

    /**
     * Get superclass <code>V1Composite</code>
     * 
     * @return <code>V1Composite</code>
     */
    public V1Composite getParent() {
        return this;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        V2Composite key = (V2Composite) other;
        if (!getId().equals(key.getId())) return false;
        if (!getSid().equals(key.getSid())) return false;
        if (!getRegion().equals(key.getRegion())) return false;
        if (!getRequestId().equals(key.getRequestId())) return false;
        if (!lang.equals(key.lang)) return false;
        return getId().equals(key.getId());  
    }

    @Override
    public int hashCode() {
        int hashCode = getId().hashCode();
        hashCode = 31 * hashCode
         + getSid().hashCode()
         + getRegion().hashCode()
         + getRequestId().hashCode()
         + lang.hashCode();
        return hashCode;
    }
}
