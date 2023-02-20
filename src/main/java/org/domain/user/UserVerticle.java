package org.domain.user;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.domain.item.ItemRouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.domain.user.UserRouteHandler.HTTP_MEDIA_JSON_TYPE;

public class UserVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(UserVerticle.class);
    public static final int HTTP_PORT = 3000;
    public static final String MONGO_CONNECTION_URL = "mongodb://localhost:27017";

    @Override
    public void start(Promise<Void> startPromise) {
        final MongoClient mongoClient = MongoClient.createShared(vertx, mongoDbConfig());

        final Router router = Router.router(vertx);
        router.route("/*")
                .handler(BodyHandler.create())
                .consumes(HTTP_MEDIA_JSON_TYPE);

        new UserRouteHandler().configUserRouter(vertx, mongoClient, router);
        final Router configuredRouter = new ItemRouteHandler().configUserRouter(vertx, mongoClient, router);

        vertx.createHttpServer()
                .requestHandler(configuredRouter)
                .listen(HTTP_PORT)
                .onSuccess(server -> log.info("Server started on port: {}", server.actualPort()));
    }


    private JsonObject mongoDbConfig() {
        return new JsonObject().put("connection_string", MONGO_CONNECTION_URL);
    }
}
