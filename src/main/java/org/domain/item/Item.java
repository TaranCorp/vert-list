package org.domain.item;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

@DataObject
public class Item {
    public static final String TABLE_NAME = "item";

    private UUID id;
    private UUID owner;
    private String name;

    public Item(UUID id, UUID owner, String name) {
        this.id = id;
        this.owner = owner;
        this.name = name;
    }

    public Item() {
    }

    public Item(JsonObject jsonObject) {
        this(
                UUID.fromString(jsonObject.getString("_id")),
                UUID.fromString(jsonObject.getString("owner")),
                jsonObject.getString("name")
        );
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("_id", id.toString())
                .put("owner", owner.toString())
                .put("name", name);
    }

    public JsonObject toResponseJson() {
        return new JsonObject()
                .put("id", id.toString())
                .put("title", name);
    }
}
