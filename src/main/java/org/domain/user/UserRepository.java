package org.domain.user;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

@ProxyGen
public interface UserRepository {

    static UserRepository create(Vertx vertx, MongoClient mongoClient) {
        return new UserRepositoryImpl(vertx, mongoClient);
    }

    static UserRepository createProxy(Vertx vertx, String address) {
        return new UserRepositoryVertxEBProxy(vertx, address);
    }

    void save(UserCredentials user, Handler<AsyncResult<Void>> resultHandler);

    void findByLogin(String login, Handler<AsyncResult<User>> resultHandler);
}
