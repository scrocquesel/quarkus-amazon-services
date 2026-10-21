package io.quarkiverse.amazon.common.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import io.quarkus.arc.ClientProxy;

/**
 * Helpers for async client beans that may be wrapped in a proxy.
 * <p>
 * This class must not depend on Vert.x, as it is used even when the Vert.x extension is absent.
 */
public final class AsyncClientProxies {

    private AsyncClientProxies() {
    }

    /**
     * Returns the actual client behind the CDI client proxy and the proxy created by this extension, if any.
     * <p>
     * Some SDK components such as the S3 Transfer Manager check the concrete type of the client they are given.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(T client) {
        client = ClientProxy.unwrap(client);
        if (client != null && Proxy.isProxyClass(client.getClass())
                && Proxy.getInvocationHandler(client) instanceof DelegatingHandler handler) {
            return (T) handler.delegate();
        }
        return client;
    }

    interface DelegatingHandler extends InvocationHandler {
        Object delegate();
    }
}
