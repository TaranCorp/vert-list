package org.domain.user;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.domain.item.ItemRouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.domain.user.UserRouteHandler.HTTP_MEDIA_JSON_TYPE;

public class UserVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(UserVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        final Router router = Router.router(vertx);
        router.route("/*")
                .handler(BodyHandler.create())
                .consumes(HTTP_MEDIA_JSON_TYPE);

        new UserRouteHandler().configUserRouter(vertx, router);
        final Router configuredRouter = new ItemRouteHandler().configUserRouter(vertx, router);
        final Integer port = 3000;
        vertx.createHttpServer()
                .requestHandler(configuredRouter)
                .listen(port, result -> {
                    if (result.succeeded()) {
                        System.out.println("Available routes: ");
                        router.getRoutes().stream().peek(route -> System.out.println(route.methods() + " - " + route.getPath())).toList();
                        log.info("Server started on port: {}", port);
                        startPromise.complete();
                    } else {
                        log.info("Something went wrong during http server initialization");
                        startPromise.fail(result.cause());
                    }
                });
    }
}
