package org.domain.user;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class UserTests {
    private static final Logger log = LoggerFactory.getLogger(UserTests.class);

    private static JsonObject credentials;

    @BeforeAll
    static void init(Vertx vertx, VertxTestContext testContext) {
        credentials = new JsonObject().put("login", UUID.randomUUID() + "@example.com").put("password", "password");
        vertx.deployVerticle(new UserVerticle(), testContext.succeeding(userVerticleId -> {
            testContext.completeNow();
        }));
    }

    @Test
    @Order(1)
    void should_returnNoContent_when_registerCredentialsAreCorrect(Vertx vertx, VertxTestContext testContext) {
        final WebClient client = WebClient.wrap(vertx.createHttpClient(
                new HttpClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(3000)
        ));

        client.post("/register")
                .sendJsonObject(credentials, testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        Assertions.assertAll(
                                () -> Assertions.assertEquals(204, response.statusCode())
                        );
                    });
                    testContext.completeNow();
                }));
    }

    @Test
    @Order(2)
    void should_returnToken_when_loginCredentialsAreCorrect(Vertx vertx, VertxTestContext testContext) {
        final WebClient client = WebClient.wrap(vertx.createHttpClient(
                new HttpClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(3000)
        ));

        client.post("/login")
                .sendJsonObject(credentials, testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        Assertions.assertAll(
                                () -> Assertions.assertEquals(200, response.statusCode()),
                                () -> Assertions.assertNotNull(response.bodyAsString())
                        );
                    });
                    testContext.completeNow();
                }));
    }
}
