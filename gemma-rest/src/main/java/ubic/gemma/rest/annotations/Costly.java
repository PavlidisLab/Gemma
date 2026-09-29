/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a resource method that costs enough to be worth bounding, and names the budget it draws on.
 *
 * <h2>What this bounds, and what it does not</h2>
 * The budget is a count of requests <em>in flight</em>, not a rate and not a per-client quota. A route
 * carrying {@code @Costly("vectors")} runs at most as many times concurrently as the {@code vectors}
 * pool has permits, whoever is asking; the surplus is answered 503 rather than queued indefinitely.
 *
 * <p>The distinction matters because the traffic this exists for does not come from one client. A
 * scraper spread over thousands of addresses trips no per-IP rate limit — every address is well
 * behaved on its own — while the aggregate still puts every connection in the pool on a matrix
 * fetch. Counting concurrency instead of requests-per-client is indifferent to how the load is
 * distributed, which is the property that makes it worth having alongside whatever the front Apache
 * is doing.
 *
 * <p>It is not a defence against a determined attacker: a full pool is a 503, and a 503 is still a
 * response. What it buys is that the database and heap see a bounded number of expensive queries
 * regardless of the arrival pattern, so the site degrades to "some requests are refused" instead of
 * "nothing responds".
 *
 * @see ubic.gemma.rest.providers.CostlyEndpointBudget
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Costly {

    /**
     * Name of the pool this route draws permits from.
     * <p>
     * Routes naming the same pool share one budget, so group routes that contend for the same
     * resource. Must be a key of {@code CostlyEndpointBudget#POOLS}; an unknown name fails fast at
     * startup rather than silently going unbounded.
     */
    String value();

    /**
     * How long a request waits for a permit before being answered 503, in seconds.
     * <p>
     * A short wait absorbs a burst that clears quickly without making the caller retry. A long one
     * just moves the queue from the client into the servlet container's thread pool, which is the
     * thing the budget exists to protect, so keep this small.
     */
    int queueSeconds() default 2;
}
