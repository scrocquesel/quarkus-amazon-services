package io.quarkiverse.amazon.common.runtime;

/**
 * Defines on which Vert.x context the futures returned by an async client are completed.
 */
public enum VertxContextPropagation {

    /**
     * Futures are completed as configured by the HTTP client, without any Vert.x context switch.
     */
    DISABLED,

    /**
     * When the call is made from a Vert.x thread, the future is completed on the caller's Vert.x context.
     * Calls made from any other thread are left untouched.
     */
    VERTX_THREADS,

    /**
     * The future is always completed on a Vert.x context: the caller's one if any, otherwise a new duplicated context.
     * This mimics the behavior of Vert.x based clients. Calls made from a worker or a virtual thread are completed on an
     * event loop.
     */
    ALWAYS
}
