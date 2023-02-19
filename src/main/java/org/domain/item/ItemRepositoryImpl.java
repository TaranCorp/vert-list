package org.domain.item;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ItemRepositoryImpl implements ItemRepository {
    private final MongoClient mongoClient;

    public ItemRepositoryImpl(Vertx vertx, MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public void save(String name, Handler<AsyncResult<Void>> resultHandler) {
        mongoClient.save(Item.TABLE_NAME, new Item(UUID.randomUUID(), UUID.randomUUID(), name).toJson(), request -> {
           if (request.succeeded()) {
               resultHandler.handle(Future.succeededFuture());
           } else {
               resultHandler.handle(Future.failedFuture(request.cause()));
           }
        });
    }

    @Override
    public void findAllByUserId(String id, Handler<AsyncResult<List<Item>>> resultHandler) {
        mongoClient.find(Item.TABLE_NAME, new JsonObject().put("owner", id), request -> {
           if (request.succeeded()) {
               resultHandler.handle(Future.succeededFuture(
                   request.result().stream()
                           .map(Item::new)
                           .toList()
               ));
           } else {
               resultHandler.handle(Future.failedFuture(request.cause()));
           }
        });
    }
}
