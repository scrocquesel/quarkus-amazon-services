package io.quarkiverse.amazon.dynamodb.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class DynamodbAsyncClientVertxContextTest {

    @Inject
    DynamoDbAsyncClient client;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("async-vertx-context-config.properties", "application.properties"));

    @Test
    public void completesOnCallerContext() throws Exception {
        assertTrue(Proxy.isProxyClass(ClientProxy.unwrap(client).getClass()));

        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        // nothing listens on the endpoint, the call fails on an SDK thread
        duplicated.runOnContext(ignored -> client.listTables().whenComplete((r, t) -> {
            completionContext.complete(Vertx.currentContext());
            failure.complete(t);
        }));

        assertNotNull(failure.get(30, TimeUnit.SECONDS));
        assertSame(duplicated, completionContext.get());
    }
}
