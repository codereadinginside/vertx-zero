package io.vertx.up.web.anima;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.up.eon.Info;
import io.vertx.up.log.Annal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared verticle method
 */
class Verticles {

    private static final ConcurrentMap<Class<?>, String> INSTANCES =
            new ConcurrentHashMap<>();

    static void deploy(final Vertx vertx,
                       final Class<?> clazz,
                       final DeploymentOptions option,
                       final Annal logger) {
        // Verticle deployment
        final String name = clazz.getName();
        final String flag = option.isWorker() ? "Worker" : "Agent";
        vertx.deployVerticle(name, option, (result) -> {
            // Success or Failed.
            if (result.succeeded()) {
                logger.info(Info.VTC_END,
                        name, option.getInstances(), result.result(),
                        flag);
                INSTANCES.put(clazz, result.result());
            } else {
                logger.warn(Info.VTC_FAIL,
                        name, option.getInstances(), result.result(),
                        null == result.cause() ? null : result.cause().getMessage(), flag);
            }
        });
    }

    static void undeploy(final Vertx vertx,
                         final Class<?> clazz,
                         final DeploymentOptions option,
                         final Annal logger) {
        // Verticle deployment
        final String name = clazz.getName();
        final String flag = option.isWorker() ? "Worker" : "Agent";
        final String id = INSTANCES.get(clazz);
        System.out.println(INSTANCES);
        System.out.println(clazz);
        System.out.println(id);
        vertx.undeploy(id, result -> {
            if (result.succeeded()) {
                logger.info(Info.VTC_STOPPED, name, id, flag);
            }
        });
    }
}
