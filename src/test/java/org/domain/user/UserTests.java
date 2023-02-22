package org.domain.user;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.domain.item.Item;
import org.domain.jwt.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class UserTests {
    private static final Logger log = LoggerFactory.getLogger(UserTests.class);

    private static JsonObject credentials;
    private static JsonObject badCredentials;
    private static WebClient client;
    private static MongoClient mongoClient;

    @BeforeAll
    static void init(Vertx vertx, VertxTestContext testContext) {
        mongoClient = MongoClient.createShared(vertx, new JsonObject().put("connection_string", "mongodb://localhost:27017"));
        mongoClient.dropCollection(User.TABLE_NAME, testContext.succeedingThenComplete());
        mongoClient.dropCollection(Item.TABLE_NAME, testContext.succeedingThenComplete());

        credentials = new JsonObject().put("login", UUID.randomUUID() + "@example.com").put("password", "password");
        badCredentials = new JsonObject().put("login", UUID.randomUUID() + "@wrong.com").put("password", "example");

        vertx.deployVerticle(new UserVerticle(), testContext.succeeding(userVerticleId -> {
            testContext.completeNow();
        }));

        client = WebClient.wrap(vertx.createHttpClient(
                new HttpClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(3000)
        ));
    }


    @AfterAll
    static void cleanUp(Vertx vertx, VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    @Order(1)
    void should_returnNoContent_when_registerCredentialsAreCorrect(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        client.post("/register")
                .sendJsonObject(credentials, testContext.succeeding(response -> {
                    assertEquals(204, response.statusCode());
                    testContext.completeNow();
                }));

        testContext.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @Test
    @Order(2)
    void should_returnToken_when_loginCredentialsAreCorrect(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        final WebClient client = WebClient.wrap(vertx.createHttpClient(
                new HttpClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(3000)
        ));

        client.post("/login")
                .sendJsonObject(credentials, testContext.succeeding(response -> {
                    assertAll(
                            () -> assertEquals(200, response.statusCode()),
                            () -> assertNotNull(response.bodyAsString())
                    );
                    testContext.completeNow();
                }));

        testContext.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @Test
    void should_returnBadCredentials_when_loginCredentialsAreIncorrectCorrect(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        final WebClient client = WebClient.wrap(vertx.createHttpClient(
                new HttpClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(3000)
        ));

        client.post("/login")
                .sendJsonObject(badCredentials, testContext.succeeding(response -> {
                    assertAll(
                            () -> assertEquals(400, response.statusCode()),
                            () -> assertNotNull(response.bodyAsString())
                    );
                    testContext.completeNow();
                }));

        testContext.awaitCompletion(5, TimeUnit.SECONDS);
    }
}
