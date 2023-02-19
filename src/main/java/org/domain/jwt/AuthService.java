package org.domain.jwt;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.domain.user.User;

public interface AuthService {
    static AuthService create(Vertx vertx) {
        return new AuthServiceImpl(vertx);
    }

    void generateToken(User user, Handler<AsyncResult<String>> resultHandler);

    void authenticate(String token, Handler<AsyncResult<io.vertx.ext.auth.User>> resultHandler);
}
