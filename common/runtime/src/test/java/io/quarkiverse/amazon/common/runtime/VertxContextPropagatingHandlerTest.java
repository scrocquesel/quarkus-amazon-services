package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class VertxContextPropagatingHandlerTest {

    public interface FakeAsyncClient {
        CompletableFuture<String> call(String value);

        CompletableFuture<String> fail();

        CompletableFuture<String> pending();

        String name();

        String broken();
    }

    private Vertx vertx;
    private ExecutorService sdkThreads;
    private CompletableFuture<String> pending;
    private FakeAsyncClient delegate;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        sdkThreads = Executors.newSingleThreadExecutor(r -> new Thread(r, "sdk-thread"));
        pending = new CompletableFuture<>();
        delegate = new FakeAsyncClient() {
            @Override
            public CompletableFuture<String> call(String value) {
                return CompletableFuture.supplyAsync(() -> value, sdkThreads);
            }

            @Override
            public CompletableFuture<String> fail() {
                return CompletableFuture.supplyAsync(() -> {
                    throw new IllegalStateException("boom");
                }, sdkThreads);
            }

            @Override
            public CompletableFuture<String> pending() {
                return pending;
            }

            @Override
            public String name() {
                return "fake";
            }

            @Override
            public String broken() {
                throw new IllegalStateException("broken");
            }
        };
    }

    @AfterEach
    void tearDown() throws Exception {
        sdkThreads.shutdownNow();
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    private FakeAsyncClient wrap(VertxContextPropagation mode) {
        return VertxContextPropagatingHandler.wrap(FakeAsyncClient.class, delegate, mode, () -> vertx);
    }

    private static final class Observed {
        Context callerContext;
        Context completionContext;
        Thread callerThread;
        Thread completionThread;
        Object value;
    }

    private Observed callFromContext(FakeAsyncClient client, Context context) throws Exception {
        Observed observed = new Observed();
        CompletableFuture<Void> done = new CompletableFuture<>();
        context.runOnContext(ignored -> {
            observed.callerContext = Vertx.currentContext();
            observed.callerThread = Thread.currentThread();
            client.call("hello").whenComplete((value, failure) -> {
                observed.completionContext = Vertx.currentContext();
                observed.completionThread = Thread.currentThread();
                observed.value = failure != null ? failure : value;
                done.complete(null);
            });
        });
        done.get(10, TimeUnit.SECONDS);
        return observed;
    }

    @Test
    void completesOnCallerEventLoopContext() throws Exception {
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());

        Observed observed = callFromContext(wrap(VertxContextPropagation.VERTX_THREADS), duplicated);

        assertEquals("hello", observed.value);
        assertSame(observed.callerContext, observed.completionContext);
        assertSame(observed.callerThread, observed.completionThread);
    }

    @Test
    void disabledDoesNotWrap() throws Exception {
        assertSame(delegate, wrap(VertxContextPropagation.DISABLED));

        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        Observed observed = callFromContext(wrap(VertxContextPropagation.DISABLED), duplicated);

        assertNull(observed.completionContext);
        assertEquals("sdk-thread", observed.completionThread.getName());
    }

    @Test
    void vertxThreadsModeLeavesOtherThreadsUntouched() throws Exception {
        FakeAsyncClient client = wrap(VertxContextPropagation.VERTX_THREADS);

        assertSame(pending, client.pending());
        assertEquals("hello", client.call("hello").get(10, TimeUnit.SECONDS));
    }

    @Test
    void alwaysModeCompletesOnEventLoopFromOtherThreads() throws Exception {
        AtomicReference<Context> completionContext = new AtomicReference<>();
        AtomicReference<Boolean> onEventLoop = new AtomicReference<>();

        String value = wrap(VertxContextPropagation.ALWAYS).call("hello")
                .whenComplete((v, t) -> {
                    completionContext.set(Vertx.currentContext());
                    onEventLoop.set(Context.isOnEventLoopThread());
                })
                .get(10, TimeUnit.SECONDS);

        assertEquals("hello", value);
        assertNotNull(completionContext.get());
        assertTrue(VertxContext.isDuplicatedContext(completionContext.get()));
        assertTrue(onEventLoop.get());
    }

    @Test
    void propagatesFailures() throws Exception {
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Object> failure = new CompletableFuture<>();
        AtomicReference<Context> completionContext = new AtomicReference<>();
        FakeAsyncClient client = wrap(VertxContextPropagation.VERTX_THREADS);

        duplicated.runOnContext(ignored -> client.fail().whenComplete((v, t) -> {
            completionContext.set(Vertx.currentContext());
            failure.complete(t);
        }));

        Throwable t = (Throwable) failure.get(10, TimeUnit.SECONDS);
        assertInstanceOf(IllegalStateException.class, t instanceof CompletionException ? t.getCause() : t);
        assertSame(duplicated, completionContext.get());
    }

    @Test
    void propagatesCancellation() throws Exception {
        CompletableFuture<CompletableFuture<String>> result = new CompletableFuture<>();
        FakeAsyncClient client = wrap(VertxContextPropagation.ALWAYS);

        vertx.getOrCreateContext().runOnContext(ignored -> result.complete(client.pending()));

        CompletableFuture<String> future = result.get(10, TimeUnit.SECONDS);
        assertNotSame(pending, future);
        future.cancel(true);
        assertTrue(pending.isCancelled());
    }

    @Test
    void passesThroughOtherMethodsAndUnwraps() {
        FakeAsyncClient client = wrap(VertxContextPropagation.VERTX_THREADS);

        assertEquals("fake", client.name());
        assertEquals(client, client);
        assertSame(delegate, AsyncClientProxies.unwrap(client));
        assertSame(delegate, AsyncClientProxies.unwrap(delegate));
        assertThrows(IllegalStateException.class, client::broken);
    }
}
