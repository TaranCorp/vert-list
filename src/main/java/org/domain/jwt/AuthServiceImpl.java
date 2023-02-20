package org.domain.jwt;

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
import org.domain.user.User;

public class AuthServiceImpl implements AuthService {

    public static final String ALGORITHM_ID = "HS256";
    public static final int TOKEN_EXPIRY_TIME = 60;
    public static final String PRIVATE_KEY = """ 
            MIICWwIBAAKBgG1nrtT5LtRf0z34OuyoyK2+1N4g4JXHQSlOdZyTjVduUo/ICJ5F
            Am62R2AED0pAGBM491HaWglBMNkBxPX7KphtagT7yhViu8tlgmGK+hJGNDTA8cjb
            kToHAA+E2MFIdyupOEU/gf6RI473J1qGm/z2QF6eyMw39XVHcU/p0zELAgMBAAEC
            gYAflSnUU2bgahVKM2mXPhZIielbgnQy6MV3zi2HmNTZo4Bz/1P1lIhiO36ZSjK7
            xYrtqalD2NvbBhW635bjR7GA1JNBLQRzqbKw7LcU1IuyxFoJgCGWTj0CtltRQDGS
            CRNMg6udqYtSvUrnhKJ7MmQKy1HRt7qxTjrBSNOUU8WRKQJBAKwwVmmtxZ1UTvUJ
            XDw7lpS6POU2b8NAL7vixXCa6kOHmPXDdmCOVMR8IN7jK/aPEeQwWMln5x2h7Dou
            """;
    private JWTAuthOptions jwtAuthOptions;
    private JWTAuth jwtAuth;

    public AuthServiceImpl(Vertx vertx) {
        jwtAuthOptions = new JWTAuthOptions()
                .setJWTOptions(
                        new JWTOptions()
                                .setExpiresInMinutes(TOKEN_EXPIRY_TIME)
                )
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm(ALGORITHM_ID)
                        .setBuffer(PRIVATE_KEY));
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
        if (token == null) {
            resultHandler.handle(Future.failedFuture(new IllegalArgumentException("Missing authorization header")));
            return;
        }
        if (!token.contains("Bearer")) {
            resultHandler.handle(Future.failedFuture(new IllegalArgumentException("Missing bearer token")));
        }
        final String clearToken = token.substring(7);
        jwtAuth.authenticate(new TokenCredentials(clearToken), request -> {
            if (request.succeeded()) {
                resultHandler.handle(Future.succeededFuture(request.result()));
            } else {
                resultHandler.handle(Future.failedFuture(request.cause()));
            }
        });
    }
}
