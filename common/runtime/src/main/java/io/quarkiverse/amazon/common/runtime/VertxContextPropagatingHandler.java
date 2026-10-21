package io.quarkiverse.amazon.common.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Completes the futures returned by an async client on the Vert.x context of the caller, so that the continuation keeps
 * running on the context the call was made from (required by e.g. Hibernate Reactive).
 * <p>
 * The AWS SDK completes futures on its own threads (the future completion executor or the HTTP client event loop), which
 * do not carry any Vert.x context.
 */
public final class VertxContextPropagatingHandler implements AsyncClientProxies.DelegatingHandler {

    private final Object delegate;
    private final VertxContextPropagation mode;
    private final Supplier<Vertx> vertx;

    private VertxContextPropagatingHandler(Object delegate, VertxContextPropagation mode, Supplier<Vertx> vertx) {
        this.delegate = delegate;
        this.mode = mode;
        this.vertx = vertx;
    }

    public static <T> T wrap(Class<T> clientInterface, T delegate, VertxContextPropagation mode) {
        return wrap(clientInterface, delegate, mode, VertxCoreRecorder.getVertx());
    }

    public static <T> T wrap(Class<T> clientInterface, T delegate, VertxContextPropagation mode, Supplier<Vertx> vertx) {
        if (mode == VertxContextPropagation.DISABLED) {
            return delegate;
        }
        return clientInterface.cast(Proxy.newProxyInstance(clientInterface.getClassLoader(),
                new Class<?>[] { clientInterface }, new VertxContextPropagatingHandler(delegate, mode, vertx)));
    }

    @Override
    public Object delegate() {
        return delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                default:
                    break;
            }
        }

        if (method.getReturnType() != CompletableFuture.class) {
            return doInvoke(method, args);
        }

        Context context = callerContext();
        CompletableFuture<?> future = (CompletableFuture<?>) doInvoke(method, args);
        if (context == null || future == null) {
            return future;
        }
        return completeOn(context, future);
    }

    private Context callerContext() {
        Context context = Vertx.currentContext();
        if (mode == VertxContextPropagation.VERTX_THREADS) {
            return context != null && Context.isOnVertxThread() ? context : null;
        }
        return context != null ? context : VertxContext.getOrCreateDuplicatedContext(vertx.get());
    }

    private Object doInvoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static <T> CompletableFuture<T> completeOn(Context context, CompletableFuture<T> source) {
        CompletableFuture<T> result = new CompletableFuture<>();
        source.whenComplete((value, failure) -> context.runOnContext(ignored -> {
            if (failure != null) {
                result.completeExceptionally(failure);
            } else {
                result.complete(value);
            }
        }));
        result.whenComplete((value, failure) -> {
            if (result.isCancelled()) {
                source.cancel(true);
            }
        });
        return result;
    }
}
