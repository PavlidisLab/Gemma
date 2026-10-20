/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.util;

import org.springframework.lang.Nullable;

/**
 * The curator an action is being taken FOR, for the duration of one call.
 *
 * <h2>Why this is not a parameter</h2>
 *
 * <p>The audit row is written by {@code AuditTrailServiceImpl} deep inside the commit's transaction,
 * reached through an {@code @Audited} aspect on a method — {@code applyDesignChange(ee, proposed)} —
 * that has no argument for it and should not grow one. Threading the name from the REST layer to the
 * audit writer would mean a new parameter on every service method between them, on every audited path,
 * to carry something almost every caller leaves null.</p>
 *
 * <p>So it is scoped to the call instead, and read at exactly one place:
 * {@code AuditTrailServiceImpl.createAuditEvent}.</p>
 *
 * <h2>🛑 It must be cleared</h2>
 *
 * <p>Threads are pooled. A name left behind is attributed to the next request that lands on the same
 * thread, which is a false entry in the permanent record of who did what — worse than no entry.
 * {@link #scope} returns an {@link AutoCloseable} so the only correct usage is the one that cleans up:</p>
 *
 * <pre>{@code
 * try ( ActingIdentity.Scope ignored = ActingIdentity.scope( actingAs ) ) {
 *     ...
 * }
 * }</pre>
 *
 * <p>Deliberately NOT inherited by child threads. Work handed to an executor is attributed to the
 * credential that ran it, which is the honest answer: nobody asked a curator about it.</p>
 */
public final class ActingIdentity {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ActingIdentity() {
    }

    /**
     * @return the curator the current call is acting for, or {@code null} — which is the ordinary case
     * and means the authenticated principal is the actor.
     */
    @Nullable
    public static String get() {
        return CURRENT.get();
    }

    /**
     * Bind {@code onBehalfOf} for the current thread until the returned scope is closed. A null or blank
     * name binds nothing, so a caller does not have to branch.
     */
    public static Scope scope( @Nullable String onBehalfOf ) {
        String previous = CURRENT.get();
        if ( onBehalfOf == null || onBehalfOf.trim().isEmpty() ) {
            CURRENT.remove();
        } else {
            CURRENT.set( onBehalfOf );
        }
        // Restores rather than clears: nesting is not expected, but a scope that clobbered an outer one
        // would silently mis-attribute the remainder of the outer call rather than fail.
        return () -> {
            if ( previous == null ) {
                CURRENT.remove();
            } else {
                CURRENT.set( previous );
            }
        };
    }

    /** Closeable that does not throw, so it can sit in a try-with-resources without a catch. */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
