package org.domain.jwt;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.domain.db.MongoVerticle;
import org.domain.user.User;
import org.domain.user.UserRepository;

public class AuthServiceImpl implements AuthService {

    private JWTAuthOptions jwtAuthOptions;
    private JWTAuth jwtAuth;
    private UserRepository userRepository;

    public AuthServiceImpl(Vertx vertx) {
        final ConfigRetriever configRetriever = ConfigRetriever.create(vertx,
                new ConfigRetrieverOptions()
                        .addStore(new ConfigStoreOptions()
                                .setType("file")
                                .setConfig(new JsonObject().put("path", "application.json"))
                        )
        );
        final Future<JsonObject> config = configRetriever.getConfig();
        config.onSuccess(request -> {
            final JsonObject jwtUtils = request.getJsonObject("jwt");
            jwtAuthOptions = new JWTAuthOptions()
                    .setJWTOptions(
                            new JWTOptions()
                                    .setExpiresInMinutes(jwtUtils.getInteger("expiry"))
                    )
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm(jwtUtils.getString("algorithm"))
                            .setBuffer(jwtUtils.getString("private_key")));
            jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
        }).onFailure(error -> {
            throw new IllegalStateException("JWT configuration not provided, Error: " + error.getMessage());
        });

        userRepository = UserRepository.createProxy(vertx, MongoVerticle.USER_REPOSITORY_ADDRESS);
    }

    public void generateToken(User user, Handler<AsyncResult<String>> resultHandler) {
        userRepository.findByLogin(user.login(), request -> {
            if (request.result() != null) {
                resultHandler.handle(Future.succeededFuture("Bearer " + jwtAuth.generateToken(
                                new JsonObject()
                                        .put("login", user.login())
                                        .put("user_id", user.id().toString())
                        )
                ));
            } else {
                resultHandler.handle(Future.failedFuture(request.cause()));
            }
        });
    }

    public void authenticate(String token, Handler<AsyncResult<io.vertx.ext.auth.User>> resultHandler) {
        if (token == null || !token.contains("Bearer")) {
            resultHandler.handle(Future.failedFuture(new IllegalArgumentException("You have not provided an authentication token, the one provided has expired, was revoked or is not authentic.")));
            return;
        }
        final String clearToken = token.substring(7);
            jwtAuth.authenticate(new TokenCredentials(clearToken), request -> {
            if (request.succeeded()) {
                userRepository.findByLogin(request.result().get("login"), userRequest -> {
                    if (userRequest.result() != null) {
                        resultHandler.handle(Future.succeededFuture(request.result()));
                    } else {
                        resultHandler.handle(Future.failedFuture(request.cause()));
                    }
                });

            } else {
                resultHandler.handle(Future.failedFuture(request.cause()));
            }
        });
    }
}
