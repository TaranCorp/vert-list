package org.domain.item;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.domain.user.UserRepository;

import java.util.UUID;

import static org.domain.user.UserVerticle.HTTP_BAD_REQUEST_CODE;
import static org.domain.user.UserVerticle.HTTP_INTERNAL_SERVER_ERROR_CODE;
import static org.domain.user.UserVerticle.HTTP_MEDIA_JSON_TYPE;
import static org.domain.user.UserVerticle.HTTP_NO_CONTENT_CODE;
import static org.domain.user.UserVerticle.HTTP_OK_CODE;
import static org.domain.user.UserVerticle.MONGO_CONNECTION_URL;

public class ItemVerticle extends AbstractVerticle {

    protected static final int HTTP_UNAUTHORIZED_CODE = 401;

    private UserRepository userRepository;
    private ItemRepository itemRepository;

    @Override
    public void start(Promise<Void> startPromise) {
        final MongoClient mongoClient = MongoClient.createShared(vertx, mongoDbConfig());
        userRepository = UserRepository.create(vertx, mongoClient);
        itemRepository = ItemRepository.create(vertx, mongoClient);

        final Router securedRouter = Router.router(vertx);
        securedRouter.route("/*")
                .handler(BodyHandler.create())
                .consumes(HTTP_MEDIA_JSON_TYPE);

        securedRouter.post("/items").handler(this::createItem);
        securedRouter.get("/items").handler(this::getItems).produces(HTTP_MEDIA_JSON_TYPE);

        vertx.createHttpServer()
                .requestHandler(securedRouter)
                .listen(9091);
    }

    private void getItems(RoutingContext context) {
        itemRepository.findAllByUserId(UUID.randomUUID().toString(), request -> {
            if (request.succeeded()) {
                responseHandle(
                        context,
                        HTTP_OK_CODE,
                        Json.encode(request.result().stream().map(Item::toResponseJson).toList())
                );
            } else {
                failureHandler(context);
            }
        });
    }

    private void createItem(RoutingContext context) {
        final JsonObject body = context.body().asJsonObject();

        if (body == null) {
            responseHandle(context, HTTP_BAD_REQUEST_CODE, "Body must be provided");
            return;
        }

        final String name = body.getString("name");

        if (name == null) {
            responseHandle(context, HTTP_BAD_REQUEST_CODE, "Name must be provided");
            return;
        }

        itemRepository.save(name, request -> {
            if (request.succeeded()) {
                responseHandle(context, HTTP_NO_CONTENT_CODE, "Item created successfull");
            } else {
                failureHandler(context);
            }
        });
    }

    private void responseHandle(RoutingContext context, int code, String msg) {
        context.response()
                .setStatusCode(code)
                .end(msg);
    }

    private void failureHandler(RoutingContext context) {
        context.response()
                .setStatusCode(HTTP_INTERNAL_SERVER_ERROR_CODE)
                .end(context.failure().getMessage());
    }

    private JsonObject mongoDbConfig() {
        return new JsonObject().put("connection_string", MONGO_CONNECTION_URL);
    }
}
