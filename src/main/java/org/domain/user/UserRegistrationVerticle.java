package org.domain.user;

import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.HashString;
import io.vertx.ext.auth.impl.hash.SHA256;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public class UserRegistrationVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(UserRegistrationVerticle.class);
    public static final int HTTP_PORT = 9090;
    public static final String SAMPLE_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiSm9obiBEb2UiLCJyb2xlIjoiU3R1ZGVudCJ9.IxBkuQHrrwJrc8_IA5DPdGhBKx43iYsricXKXUQt_8o";
    public static final int HTTP_NO_CONTENT_CODE = 204;
    public static final int HTTP_CONFLICT_CODE = 409;
    public static final int HTTP_OK_CODE = 200;
    public static final int HTTP_BAD_REQUEST_CODE = 400;
    public static final String HTTP_MEDIA_JSON_TYPE = "application/json";
    public static final int HTTP_INTERNAL_SERVER_ERROR_CODE = 500;
    public static final int REQUIRED_PASSWORD_LENGTH = 8;
    public static final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
    public static final String MONGO_CONNECTION_URL = "mongodb://localhost:27017";

    private UserRepository userRepository;

    @Override
    public void start(Promise<Void> startPromise) {
        userRepository = UserRepository.create(vertx, MongoClient.createShared(vertx, mongoDbConfig())); // TODO should delegate mongo creation to another verticle

        final Router router = Router.router(vertx);
        router.route("/*")
                .handler(BodyHandler.create())
                .consumes(HTTP_MEDIA_JSON_TYPE);

        router.post("/login").handler(this::login).produces(HTTP_MEDIA_JSON_TYPE).failureHandler(this::failureHandler);
        router.post("/register").handler(this::register).produces(HTTP_MEDIA_JSON_TYPE).failureHandler(this::failureHandler);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(HTTP_PORT)
                .onSuccess(server -> log.info("Server started on port: {}", server.actualPort()));
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
                        responseHandle(context, HTTP_NO_CONTENT_CODE, "");
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
                    sendToken(context);
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

    private void sendToken(RoutingContext context) {
        context.response()
                .setStatusCode(HTTP_OK_CODE);
        context.json(new JsonObject().put("token", SAMPLE_TOKEN));
    }

    private UserCredentials parseCredentials(RequestBody body) {
        final JsonObject userAsJson = body.asJsonObject();

        if (userAsJson == null) {
            throw new IllegalArgumentException("Body must be provided");
        }

        final String login = deleteWhitespace(userAsJson.getString("login")).toLowerCase(Locale.ROOT);
        final String password = userAsJson.getString("password");

        if (password.length() < REQUIRED_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain at least %s characters".formatted(REQUIRED_PASSWORD_LENGTH));
        }
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

    private JsonObject mongoDbConfig() {
        return new JsonObject().put("connection_string", MONGO_CONNECTION_URL);
    }

    private String encrypt(String password) {
        SHA256 sha256 = new SHA256();
        return sha256.hash(new HashString(sha256.id(), Map.of(), ""), password);
    }
}
