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

    public User(String login, String password) {
        this.login = login;
        this.password = password;
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

    public void setId(UUID id) {
        this.id = id;
    }

    public String login() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String password() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("_id", id.toString())
                .put("login", login)
                .put("password", password);
    }
}
