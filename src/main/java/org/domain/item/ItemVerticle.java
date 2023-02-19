package org.domain.item;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.domain.jwt.AuthService;

import java.util.UUID;

import static org.domain.user.UserVerticle.HTTP_BAD_REQUEST_CODE;
import static org.domain.user.UserVerticle.HTTP_INTERNAL_SERVER_ERROR_CODE;
import static org.domain.user.UserVerticle.HTTP_MEDIA_JSON_TYPE;
import static org.domain.user.UserVerticle.HTTP_NO_CONTENT_CODE;
import static org.domain.user.UserVerticle.HTTP_OK_CODE;
import static org.domain.user.UserVerticle.MONGO_CONNECTION_URL;

public class ItemVerticle extends AbstractVerticle {

    protected static final int HTTP_UNAUTHORIZED_CODE = 401;

    private ItemRepository itemRepository;
    private AuthService authService;

    @Override
    public void start(Promise<Void> startPromise) {
        final MongoClient mongoClient = MongoClient.createShared(vertx, mongoDbConfig());
        itemRepository = ItemRepository.create(vertx, mongoClient);
        authService = AuthService.create(vertx);

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
        authService.authenticate(context.request().getHeader("Authorization"), request -> {
            if (request.succeeded()) {
                itemRepository.findAllByUserId(request.result().get("user_id"), itemRequest -> {
                    if (itemRequest.succeeded()) {
                        responseHandle(
                                context,
                                HTTP_OK_CODE,
                                Json.encode(itemRequest.result().stream().map(ItemResponse::create).toList())
                        );
                    } else {
                        failureHandler(context);
                    }
                });
            } else {
                unauthorizedHandler(context);
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

        authService.authenticate(context.request().getHeader("Authorization"), request -> {
            if (request.succeeded()) {
                itemRepository.save(new Item(UUID.randomUUID(), UUID.fromString(request.result().get("user_id")), name), itemRequest -> {
                    if (itemRequest.succeeded()) {
                        responseHandle(context, HTTP_NO_CONTENT_CODE, "Item created successfull");
                    } else {
                        failureHandler(context);
                    }
                });
            } else {
                unauthorizedHandler(context);
            }
        });
    }

    private void unauthorizedHandler(RoutingContext context) {
        responseHandle(context, HTTP_UNAUTHORIZED_CODE, "You have not provided an authentication token, the one provided has expired, was revoked or is not authentic");
    }

    private void responseHandle(RoutingContext context, int code, String msg) {
        context.response()
                .setStatusCode(code)
                .end(msg);
    }

    private void failureHandler(RoutingContext context) {
        Throwable failure = context.failure();
        if (failure != null) {
            context.response()
                    .setStatusCode(HTTP_INTERNAL_SERVER_ERROR_CODE)
                    .end(failure.getMessage());
        }
        context.response().setStatusCode(HTTP_INTERNAL_SERVER_ERROR_CODE).end();
    }

    private JsonObject mongoDbConfig() {
        return new JsonObject().put("connection_string", MONGO_CONNECTION_URL);
    }
}
