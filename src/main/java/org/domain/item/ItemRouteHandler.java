package org.domain.item;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.domain.jwt.AuthService;

import java.util.UUID;

import static org.domain.user.UserRouteHandler.HTTP_BAD_REQUEST_CODE;
import static org.domain.user.UserRouteHandler.HTTP_INTERNAL_SERVER_ERROR_CODE;
import static org.domain.user.UserRouteHandler.HTTP_MEDIA_JSON_TYPE;
import static org.domain.user.UserRouteHandler.HTTP_NO_CONTENT_CODE;
import static org.domain.user.UserRouteHandler.HTTP_OK_CODE;

public class ItemRouteHandler {

    protected static final int HTTP_UNAUTHORIZED_CODE = 401;

    private ItemRepository itemRepository;
    private AuthService authService;

    public Router configUserRouter(Vertx vertx, MongoClient mongoClient, Router router) {
        itemRepository = ItemRepository.create(vertx, mongoClient);
        authService = AuthService.create(vertx);

        router.post("/items").handler(this::createItem);
        router.get("/items").handler(this::getItems).produces(HTTP_MEDIA_JSON_TYPE);

        return router;
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
}
