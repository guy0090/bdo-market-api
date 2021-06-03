package io.arsha.api.cache;

import io.vertx.core.json.JsonObject;

public final class UtilKey {
    /**
     * Composite key for utility cache
     * 
     * @param collection the collection to search
     * @param query      the query to run 
     */
    public UtilKey(final String collection, final JsonObject query) {
        this.collection = collection;
        this.query = query;
    }

    private String collection;
    private JsonObject query;

    public String getCollection() {
        return this.collection;
    }

    public JsonObject getQuery() {
        return this.query;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        UtilKey key = (UtilKey) other;
        if (!collection.equals(key.collection)) return false;
        if (!query.equals(key.query)) return false;

        return collection.equals(key.collection);  
    }

    @Override
    public int hashCode() {
        int hashCode = collection.hashCode();
        hashCode = 31 * hashCode
         + query.hashCode();
        return hashCode;
    }
}
