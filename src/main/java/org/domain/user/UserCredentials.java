package org.domain.user;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class UserCredentials {
    private String login;
    private String password;

    public UserCredentials(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public UserCredentials(JsonObject jsonObject) {
        this(
            jsonObject.getString("login"),
            jsonObject.getString("password")
        );
    }

    public UserCredentials() {
    }

    public String login() {
        return login;
    }

    public String password() {
        return password;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("login", login)
                .put("password", password);
    }
}
