package org.domain.user;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.UUID;

public class UserRepositoryImpl implements UserRepository {

    private final MongoClient mongoClient;

    public UserRepositoryImpl(Vertx vertx, MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public void save(UserCredentials credentials, Handler<AsyncResult<Void>> resultHandler) {
        mongoClient.save(
            User.TABLE_NAME,
            new User(UUID.randomUUID(), credentials.login(), credentials.password()).toJson(),
            result -> {
                if (result.succeeded()) {
                    System.out.println(result.result());
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    System.out.println(result.cause().getMessage());
                    resultHandler.handle(Future.failedFuture(result.cause()));
                }
            });
    }

    @Override
    public void findByLogin(String login, Handler<AsyncResult<User>> resultHandler) {
        mongoClient.find(User.TABLE_NAME, new JsonObject().put("login", login), handler -> {
            if (handler.succeeded()) {
                final List<JsonObject> result = handler.result();
                if (result.isEmpty()) {
                    resultHandler.handle(Future.succeededFuture(null));
                } else {
                    final JsonObject entries = result.stream().findFirst().get();
                    resultHandler.handle(Future.succeededFuture(new User(entries)));
                }
            } else {
                resultHandler.handle(Future.failedFuture(handler.cause()));
            }
        });
    }
}
