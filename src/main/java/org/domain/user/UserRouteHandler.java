package org.domain.user;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashString;
import io.vertx.ext.auth.impl.hash.SHA256;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.domain.db.MongoVerticle;
import org.domain.jwt.AuthService;

import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public class UserRouteHandler {
    public static final int HTTP_NO_CONTENT_CODE = 204;
    public static final int HTTP_CONFLICT_CODE = 409;
    public static final int HTTP_OK_CODE = 200;
    public static final int HTTP_BAD_REQUEST_CODE = 400;
    public static final int HTTP_INTERNAL_SERVER_ERROR_CODE = 500;
    public static final int HTTP_UNAUTHORIZED_CODE = 401;
    public static final String HTTP_MEDIA_JSON_TYPE = "application/json";
    public static final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
    public static final String SALT = ")PUR/6DboouA";

    private UserRepository userRepository;
    private AuthService authService;

    public Router configUserRouter(Vertx vertx, Router router) {
        userRepository = UserRepository.createProxy(vertx, MongoVerticle.USER_REPOSITORY_ADDRESS);
        authService = AuthService.create(vertx);

        router.route("/*")
                .handler(BodyHandler.create())
                .consumes(HTTP_MEDIA_JSON_TYPE);

        router.post("/login").handler(this::login).produces(HTTP_MEDIA_JSON_TYPE).failureHandler(this::failureHandler);
        router.post("/register").handler(this::register).produces(HTTP_MEDIA_JSON_TYPE).failureHandler(this::failureHandler);

        return router;
    }

    private void failureHandler(RoutingContext context) {
        context.response()
                .setStatusCode(HTTP_INTERNAL_SERVER_ERROR_CODE)
                .end(context.failure().getMessage());
    }

    private void register(RoutingContext context) {
        final UserCredentials userCredentials = parseCredentials(context.body());

        userRepository.findByLogin(userCredentials.login(), handler -> {
            if (handler.succeeded()) {
                if (handler.result() != null) {
                    responseHandle(context, HTTP_CONFLICT_CODE, "User with login %s already exists".formatted(userCredentials.login()));
                    return;
                }
                userCredentials.setPassword(encrypt(userCredentials.password()));
                userRepository.save(userCredentials, saveResult -> {
                    if (handler.succeeded()) {
                        responseHandle(context, HTTP_NO_CONTENT_CODE, "Registering successfull");
                    } else {
                        handleFail(context, handler.cause());
                    }
                });
            } else {
                handleFail(context, handler.cause());
            }
        });
    }

    private void login(RoutingContext context) {
        final UserCredentials userCredentials = parseCredentials(context.body());

        userRepository.findByLogin(userCredentials.login().toLowerCase(Locale.ROOT), handler -> {
            if (handler.succeeded()) {
                final User result = handler.result();
                if (result != null && matchPasswords(userCredentials.password(), result.password())) {
                    sendToken(context, result);
                } else {
                    responseHandle(context, HTTP_BAD_REQUEST_CODE, "Bad credentials");
                }
            }
            else {
                handleFail(context, handler.cause());
            }
        });
    }

    private void responseHandle(RoutingContext context, int code, String msg) {
        context.response()
                .setStatusCode(code)
                .end(msg);
    }

    private void handleFail(RoutingContext context, Throwable cause) {
        context.fail(HTTP_INTERNAL_SERVER_ERROR_CODE, cause);
    }

    private void sendToken(RoutingContext context, User user) {
        authService.generateToken(user, request -> {
            if (request.succeeded()) {
                context.response()
                        .setStatusCode(HTTP_OK_CODE);
                context.json(new JsonObject().put("token", request.result()));
            } else {
                handleFail(context, request.cause());
            }
        });

    }

    private UserCredentials parseCredentials(RequestBody body) {
        final JsonObject userAsJson = body.asJsonObject();

        if (userAsJson == null) {
            throw new IllegalArgumentException("Body must be provided");
        }

        final String login = deleteWhitespace(userAsJson.getString("login")).toLowerCase(Locale.ROOT);
        final String password = userAsJson.getString("password");

        if (!login.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Provided email is incorrect");
        }
        if (StringUtil.isNullOrEmpty(login)) {
            throw new IllegalArgumentException("Login must be provided");
        }
        if (StringUtil.isNullOrEmpty(password)) {
            throw new IllegalArgumentException("Password must be provided");
        }

        return new UserCredentials(login, password);
    }

    private boolean matchPasswords(String providedPassword, String currentPassword) {
        return encrypt(providedPassword).equals(currentPassword);
    }

    private String encrypt(String password) {
        SHA256 sha256 = new SHA256();
        return sha256.hash(new HashString(sha256.id(), Map.of(), SALT), password);
    }
}
