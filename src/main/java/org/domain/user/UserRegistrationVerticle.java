package org.domain.user;

import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected static final int HTTP_INTERNAL_SERVER_ERROR_CODE = 500;

    private UserRepository userRepository;

    @Override
    public void start(Promise<Void> startPromise) {
        userRepository = UserRepository.create(vertx, MongoClient.createShared(vertx, mongoDbConfig()));

        final Router router = Router.router(vertx);
        router.route("/*")
                .handler(BodyHandler.create())
                .consumes(HTTP_MEDIA_JSON_TYPE);

        router.post("/login").handler(this::login).produces(HTTP_MEDIA_JSON_TYPE).failureHandler(this::failureHandler);
        router.post("/register").handler(this::register).failureHandler(this::failureHandler);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(HTTP_PORT)
                .onSuccess(server -> {
                    log.info("Server started on port: {}", server.actualPort());
                });
    }

    private void failureHandler(RoutingContext context) {
        final String errorMsg = context.failure().getMessage();
        context.response()
                .setStatusCode(HTTP_INTERNAL_SERVER_ERROR_CODE)
                .setStatusMessage(errorMsg)
                .end(errorMsg);
    }

    private void register(RoutingContext context) {
        final UserCredentials userCredentials = parseCredentials(context.body(), context);
        // hash password // strict password

        userRepository.findByLogin(userCredentials.login(), handler -> {
            if (handler.succeeded()) {
                if (handler.result() != null) {
                    responseHandle(context, HTTP_CONFLICT_CODE, "User with login %s already exists".formatted(userCredentials.login()));
                    return;
                }
                userRepository.save(userCredentials, saveResult -> {
                    if (handler.succeeded()) {
                        responseHandle(context, HTTP_NO_CONTENT_CODE, "Registering successfully");
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
        final UserCredentials userCredentials = parseCredentials(context.body(), context);

        userRepository.findByLogin(userCredentials.login(), handler -> {
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
                .setStatusMessage(msg)
                .end();
    }

    private void handleFail(RoutingContext context, Throwable cause) {
        context.fail(HTTP_INTERNAL_SERVER_ERROR_CODE, cause);
    }

    private boolean matchPasswords(String providedPassword, String currentPassword) {
        return currentPassword.equals(providedPassword);
    }

    private void sendToken(RoutingContext context) {
        context.response()
                .setStatusCode(HTTP_OK_CODE);
        context.json(new JsonObject().put("token", SAMPLE_TOKEN));
    }

    private UserCredentials parseCredentials(RequestBody body, RoutingContext context) {
        final JsonObject userAsJson = body.asJsonObject();

        if (userAsJson == null) {
            throw new IllegalArgumentException("Body must be provided");
        }

        final String login = deleteWhitespace(userAsJson.getString("login"));
        final String password = userAsJson.getString("password");

        if (StringUtil.isNullOrEmpty(login)) {
            throw new IllegalArgumentException("Login must be provided");
        }
        if (StringUtil.isNullOrEmpty(password)) {
            throw new IllegalArgumentException("Password must be provided");
        }

        // check email regexp

        return new UserCredentials(login, password);
    }

    private JsonObject mongoDbConfig() {
        return new JsonObject().put("connection_string", System.getenv().get("datasource.url"));
    }
}
