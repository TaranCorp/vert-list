package org.domain;

import io.vertx.core.Vertx;
import org.domain.user.UserVerticle;

public class VertListApp {

    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new UserVerticle());
    }

}
