package org.domain.item;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

@ProxyGen
public interface ItemRepository {
    static ItemRepository create(Vertx vertx, MongoClient mongoClient) {
        return new ItemRepositoryImpl(vertx, mongoClient);
    }

    void save(String name, Handler<AsyncResult<Void>> resultHandler);

    void findAllByUserId(String id, Handler<AsyncResult<List<Item>>> resultHandler);
}
