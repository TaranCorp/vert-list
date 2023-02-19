package org.domain.item;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class Item {
    public Item() {
    }

    public Item(JsonObject jsonObject) {
    }

    JsonObject toJson() {
        return new JsonObject();
    }
}
