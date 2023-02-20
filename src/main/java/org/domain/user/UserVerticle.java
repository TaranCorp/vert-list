package org.domain.user;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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

    @Override
    public void start(Promise<Void> startPromise) {
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx,
                new ConfigRetrieverOptions()
                        .addStore(new ConfigStoreOptions()
                                .setType("file")
                                .setConfig(new JsonObject().put("path", "application.json"))
                        )
        );
        Future<JsonObject> config = configRetriever.getConfig();

        config.onSuccess(request -> {
            JsonObject datasource = request.getJsonObject("datasource");
            final JsonObject dbConfig = new JsonObject();
            dbConfig.put("host", datasource.getString("host"));
            dbConfig.put("port", datasource.getInteger("port"));

            final MongoClient mongoClient = MongoClient.createShared(vertx, dbConfig);

            final Router router = Router.router(vertx);
            router.route("/*")
                    .handler(BodyHandler.create())
                    .consumes(HTTP_MEDIA_JSON_TYPE);

            new UserRouteHandler().configUserRouter(vertx, mongoClient, router);
            final Router configuredRouter = new ItemRouteHandler().configUserRouter(vertx, mongoClient, router);

            vertx.createHttpServer()
                    .requestHandler(configuredRouter)
                    .listen(HTTP_PORT, result -> {
                        if (result.succeeded()) {
                            log.info("Server started on port: {}", HTTP_PORT);
                            startPromise.complete();
                        } else {
                            log.info("Something went wrong during http server initialization");
                            startPromise.fail(result.cause());
                        }
                    });
        })
        .onFailure(startPromise::fail);
    }
}
