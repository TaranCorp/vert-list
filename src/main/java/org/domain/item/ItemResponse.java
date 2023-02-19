package org.domain.item;

import io.vertx.core.json.JsonObject;

public class ItemResponse {
    public static JsonObject create(Item item) {
        return new JsonObject()
                .put("id", item.id().toString())
                .put("title", item.name());
    }
}
