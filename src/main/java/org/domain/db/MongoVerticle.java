package org.domain.db;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.domain.item.ItemRepository;
import org.domain.user.UserRepository;

public class MongoVerticle extends AbstractVerticle {

    public static final String ITEM_REPOSITORY_ADDRESS = "item-repository-address";
    public static final String USER_REPOSITORY_ADDRESS = "user-repository-address";

    @Override
    public void start(Promise<Void> startPromise) {
        final ConfigRetriever configRetriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(getApplicationProperties()));
        configRetriever.getConfig()
            .onSuccess(properties -> {
                final JsonObject datasource = properties.getJsonObject("datasource");
                final JsonObject dbConfig = new JsonObject()
                        .put("host", datasource.getString("host"))
                        .put("port", datasource.getInteger("port"));

                final MongoClient mongoClient = MongoClient.createShared(vertx, dbConfig);

                ServiceBinder serviceBinder = new ServiceBinder(vertx);

                serviceBinder.setAddress(USER_REPOSITORY_ADDRESS)
                        .register(UserRepository.class, UserRepository.create(vertx, mongoClient));
                serviceBinder.setAddress(ITEM_REPOSITORY_ADDRESS)
                        .register(ItemRepository.class, ItemRepository.create(vertx, mongoClient));
            })
            .onFailure(startPromise::fail);
    }

    private ConfigStoreOptions getApplicationProperties() {
        return new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", "application.json"));
    }
}
