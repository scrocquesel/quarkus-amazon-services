package io.quarkiverse.amazon.dynamodb.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Proxy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class DynamodbAsyncClientVertxContextDisabledTest {

    @Inject
    DynamoDbAsyncClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("async-vertx-context-disabled-config.properties", "application.properties"));

    @Test
    public void clientIsNotWrapped() {
        assertFalse(Proxy.isProxyClass(ClientProxy.unwrap(client).getClass()));
    }
}
