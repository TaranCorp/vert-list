package org.domain.jwt;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.hash.SHA256;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.domain.user.User;

public class AuthServiceImpl implements AuthService {

    private JWTAuthOptions jwtAuthOptions;
    private JWTAuth jwtAuth;

    public AuthServiceImpl(Vertx vertx) {
        jwtAuthOptions = new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("keyboard cat"));
        jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
    }

    public void generateToken(User user, Handler<AsyncResult<String>> resultHandler) {
            resultHandler.handle(Future.succeededFuture("Bearer " + jwtAuth.generateToken(
                    new JsonObject()
                            .put("login", user.login())
                            .put("user_id", user.id().toString())
                    )
            ));
    }

    public void authenticate(String token, Handler<AsyncResult<io.vertx.ext.auth.User>> resultHandler) {
        final String clearToken = token.contains("Bearer") ? token.substring(7) : token;
        jwtAuth.authenticate(new TokenCredentials(clearToken), request -> {
            if (request.succeeded()) {
                resultHandler.handle(Future.succeededFuture(request.result()));
            } else {
                resultHandler.handle(Future.failedFuture(request.cause()));
            }
        });
    }
}
