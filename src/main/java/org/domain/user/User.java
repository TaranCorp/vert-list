package org.domain.user;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

@DataObject
public class User {
    public static final String TABLE_NAME = "user";

    private UUID id;
    private String login;
    private String password;

    public User() {
    }

    public User(UUID id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    public User(JsonObject jsonObject) {
        this(
            UUID.fromString(jsonObject.getString("_id")),
            jsonObject.getString("login"),
            jsonObject.getString("password")
        );
    }

    public UUID id() {
        return id;
    }

    public String login() {
        return login;
    }

    public String password() {
        return password;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("_id", id.toString())
                .put("login", login)
                .put("password", password);
    }
}
