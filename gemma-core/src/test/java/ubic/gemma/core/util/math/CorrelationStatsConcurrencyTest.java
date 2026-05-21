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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package ubic.gemma.core.util.math;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression guard for the static {@link CorrelationStats} p-value lookup caches.
 * <p>
 * The Pearson and Spearman caches are backed by {@code cern.colt.matrix.impl.SparseDoubleMatrix2D}, which is not
 * thread-safe. Prior to the monitor-locking fix (commit {@code 479329c229}), concurrent {@code setQuick} from
 * multiple correlation-computing threads could corrupt the sparse matrix's internal hash table — surfacing as
 * {@link NullPointerException} from a subsequent {@code getQuick}, or as silently wrong values when threads
 * disagreed on the value for a given input.
 * <p>
 * The test design:
 * <ol>
 *   <li>Build a set of {@code (correlation, sampleSize)} inputs with disjoint cache bins (so each input maps to a
 *       distinct {@code (bin, dof)} slot in the underlying matrix).</li>
 *   <li>Spawn N threads that all hammer the same set of inputs from cold. With the cache cold and N threads racing
 *       to write, the first writer wins per slot and all subsequent writes overwrite with the same value — but the
 *       writes themselves can collide on the sparse matrix's internal hash table.</li>
 *   <li>Assert no exception, AND that every returned value matches a reference computed via the same math but
 *       outside the cache, AND that all threads agree on the value for each input (no slot returns different values
 *       on different reads).</li>
 * </ol>
 */
class CorrelationStatsConcurrencyTest {

    private static final int THREAD_COUNT = 16;
    private static final int ITERATIONS = 10_000;
    private static final long RNG_SEED = 0xC0FFEEL;
    private static final double TOLERANCE = 1e-9;

    /** Inputs every thread hits — concurrent first-writer races on the same cache slot. */
    private static final int HOT_INPUTS = 20;
    /** Additional inputs split across threads — drives the matrix backing-store past its initial capacity. */
    private static final int WARM_INPUTS = 200;

    /** Bin size in {@link CorrelationStats}. Mirrored here so we can produce bin-aligned inputs. */
    private static final double BINSIZE = 0.005;

    /**
     * Deterministic (correlation, sampleSize) input pair. The correlation is always bin-aligned ({@code bin * BINSIZE}),
     * so the cached value for this input is the exact p-value computed for this correlation — no discretization
     * mismatch with a direct-math reference.
     */
    private static final class Input {
        final double correl;
        final int n;

        Input( double correl, int n ) {
            this.correl = correl;
            this.n = n;
        }

        @Override
        public boolean equals( Object o ) {
            if ( !( o instanceof Input ) ) return false;
            Input other = ( Input ) o;
            return Double.compare( correl, other.correl ) == 0 && n == other.n;
        }

        @Override
        public int hashCode() {
            return Double.hashCode( correl ) * 31 + n;
        }

        @Override
        public String toString() {
            return "Input{correl=" + correl + ", n=" + n + "}";
        }
    }

    /**
     * Build inputs with disjoint {@code (bin, dof)} cache slots so each input has a unique slot in the underlying
     * sparse matrix. Without this, two distinct correlations could hash to the same bin and disagree on which
     * p-value the cache should hold — making the assertion "got == reference" ambiguous.
     */
    private static List<Input> buildInputs( long seed ) {
        Random rng = new Random( seed );
        // 12 sample sizes × at least 20 distinct bins per sample size = >= 240 unique slots, which is plenty.
        int[] sampleSizes = { 5, 8, 9, 12, 20, 30, 50, 100, 200, 500, 800, 999 };
        // Bins 3..199 are safe — bin 0,1,2 are edge-near-zero, bin 200 is edge-near-one (correlation = 1.0 triggers
        // the short-circuit path that bypasses the cache).
        Set<Long> usedSlots = new java.util.HashSet<>();
        LinkedHashSet<Input> uniq = new LinkedHashSet<>();
        int target = HOT_INPUTS + WARM_INPUTS;
        int safety = 0;
        while ( uniq.size() < target && safety++ < 100_000 ) {
            int bin = 3 + rng.nextInt( 195 ); // bins 3..197
            int n = sampleSizes[rng.nextInt( sampleSizes.length )];
            int dof = n - 2;
            long slot = ( long ) bin * 100_000L + dof;
            if ( !usedSlots.add( slot ) ) continue;
            // bin-aligned correlation: ceil(correl/BINSIZE) == bin iff correl is in ((bin-1)*BINSIZE, bin*BINSIZE].
            // Use the right endpoint so the input is the canonical representative of its bin.
            double correl = bin * BINSIZE;
            uniq.add( new Input( correl, n ) );
        }
        if ( uniq.size() < target ) {
            throw new IllegalStateException( "Could not generate " + target + " unique inputs (got " + uniq.size() + ")" );
        }
        return new ArrayList<>( uniq );
    }

    /**
     * Pearson p-value: concurrent callers don't corrupt the cache and all return the same correct value.
     */
    @Test
    void concurrentPearsonPvalue_returnsSameValuesAsSingleThreaded() throws Exception {
        List<Input> inputs = buildInputs( RNG_SEED );
        // Reference values via direct math — does NOT touch the cache, so the storm starts cold.
        Map<Input, Double> reference = new HashMap<>();
        for ( Input in : inputs ) {
            reference.put( in, pearsonReferencePvalue( in.correl, in.n ) );
        }
        runConcurrent( inputs, reference, /* spearman */ false );
    }

    /**
     * Spearman p-value: concurrent callers don't corrupt the cache and all return the same correct value.
     */
    @Test
    void concurrentSpearmanPvalue_returnsSameValuesAsSingleThreaded() throws Exception {
        // Different seed than the Pearson test so the two tests don't share cache slots (test-method order is not
        // guaranteed; sharing slots would mean the second test sees the first's storm-populated cache and never
        // exercises a cold write).
        List<Input> inputs = buildInputs( RNG_SEED ^ 0xDEADBEEFL );
        // Reference values via CorrelationStats.spearmanPvalue itself — this WILL populate the cache, so the storm
        // is read-only. That's still a useful concurrency test (concurrent getQuick on a sparse matrix that's being
        // grown elsewhere is also the race in the bug report), and it pins the cached values.
        // To exercise the write race more aggressively, we follow with a second storm on a *different* set of
        // inputs without precomputing references for them — see below.
        Map<Input, Double> reference = new HashMap<>();
        for ( Input in : inputs ) {
            reference.put( in, CorrelationStats.spearmanPvalue( in.correl, in.n ) );
        }
        runConcurrent( inputs, reference, /* spearman */ true );

        // Second wave: cold-cache write race. Generate a disjoint input set, do NOT precompute references; instead,
        // run the concurrent storm and assert all threads agree on the value for each input (consensus). This
        // catches the corruption-causes-different-threads-to-see-different-values failure mode without needing a
        // pre-storm reference.
        List<Input> coldInputs = buildInputs( RNG_SEED ^ 0xFEEDFACEL );
        runConcurrentConsensus( coldInputs, /* spearman */ true );
    }

    /**
     * Pearson cold-storm consensus check: no pre-storm reference, just N threads racing on a cold cache; assert
     * every thread agrees on the value for each input. Exercises the cache-WRITE race that the Pearson reference-
     * via-direct-math test path doesn't quite hit (the reference path is exact, but the storm is also exact, so
     * "got != reference" only fires on actual corruption — this consensus path adds the agree-with-each-other
     * check).
     */
    @Test
    void concurrentPearsonPvalue_threadsAgreeOnConsensusValue() throws Exception {
        List<Input> inputs = buildInputs( RNG_SEED ^ 0xCAFEBABEL );
        runConcurrentConsensus( inputs, /* spearman */ false );
    }

    /**
     * Reference Pearson p-value, computed directly from the t-distribution. Mirrors the math in
     * {@link CorrelationStats#pvalue} without touching the static cache.
     */
    private static double pearsonReferencePvalue( double correl, int count ) {
        double acorrel = Math.abs( correl );
        if ( acorrel == 1.0 ) return 0.0;
        if ( acorrel == 0.0 ) return 1.0;
        int dof = count - 2;
        if ( dof <= 0 ) return 1.0;
        double t = acorrel / Math.sqrt( ( 1.0 - acorrel * acorrel ) / dof );
        return cern.jet.stat.Probability.studentT( dof, -t );
    }

    private static void runConcurrent( List<Input> inputs, Map<Input, Double> reference, boolean spearman )
            throws Exception {
        List<Input> hot = inputs.subList( 0, HOT_INPUTS );
        List<Input> warm = inputs.subList( HOT_INPUTS, inputs.size() );

        ExecutorService pool = Executors.newFixedThreadPool( THREAD_COUNT );
        try {
            CountDownLatch start = new CountDownLatch( 1 );
            AtomicReference<Throwable> firstError = new AtomicReference<>();
            List<Future<?>> futures = new ArrayList<>( THREAD_COUNT );

            for ( int t = 0; t < THREAD_COUNT; t++ ) {
                final int threadId = t;
                // Each thread gets a slice of warm inputs plus all hot inputs.
                int sliceSize = Math.max( 1, warm.size() / THREAD_COUNT );
                int from = ( threadId * sliceSize ) % Math.max( 1, warm.size() );
                int to = Math.min( from + sliceSize, warm.size() );
                List<Input> threadWarm = new ArrayList<>( warm.subList( from, to ) );
                Collections.shuffle( threadWarm, new Random( RNG_SEED + threadId ) );

                futures.add( pool.submit( () -> {
                    try {
                        start.await();
                        Random localRng = new Random( RNG_SEED + threadId * 7919L );
                        for ( int i = 0; i < ITERATIONS; i++ ) {
                            Input in;
                            if ( localRng.nextInt( 10 ) < 7 ) {
                                in = hot.get( localRng.nextInt( hot.size() ) );
                            } else {
                                in = threadWarm.get( localRng.nextInt( threadWarm.size() ) );
                            }
                            double got = spearman
                                    ? CorrelationStats.spearmanPvalue( in.correl, in.n )
                                    : CorrelationStats.pvalue( in.correl, in.n );
                            Double expected = reference.get( in );
                            if ( expected == null ) {
                                firstError.compareAndSet( null,
                                        new AssertionError( "missing reference for " + in ) );
                                return;
                            }
                            if ( Double.isNaN( got ) != Double.isNaN( expected ) ) {
                                firstError.compareAndSet( null, new AssertionError(
                                        "NaN mismatch for " + in + ": expected " + expected + ", got " + got ) );
                                return;
                            }
                            if ( !Double.isNaN( got ) && Math.abs( got - expected ) > TOLERANCE ) {
                                firstError.compareAndSet( null, new AssertionError(
                                        "value mismatch for " + in + ": expected " + expected + ", got " + got
                                                + " (delta " + Math.abs( got - expected ) + ")" ) );
                                return;
                            }
                        }
                    } catch ( Throwable th ) {
                        // NPE, ArithmeticException, or anything else thrown by corrupted matrix state.
                        firstError.compareAndSet( null, th );
                    }
                } ) );
            }

            start.countDown();
            for ( Future<?> f : futures ) {
                f.get( 60, TimeUnit.SECONDS );
            }

            Throwable err = firstError.get();
            assertNull( err, () -> "concurrent corruption: "
                    + ( err == null ? "" : err.getClass().getSimpleName() + ": " + err.getMessage() ) );
        } finally {
            pool.shutdownNow();
        }

        // Post-storm sanity: every cache slot still returns the value that matches the reference.
        for ( Input in : inputs ) {
            double expected = reference.get( in );
            double after = spearman
                    ? CorrelationStats.spearmanPvalue( in.correl, in.n )
                    : CorrelationStats.pvalue( in.correl, in.n );
            assertEquals( expected, after, TOLERANCE, () -> "cache slot corrupted post-storm for " + in );
        }
    }

    /**
     * Cold-cache consensus storm: N threads race to populate the cache. Per input, record the FIRST value each
     * thread sees, then assert all threads saw the same value (no NaN, no NPE, no thread-local divergence). Catches
     * corruption that surfaces as silently wrong values rather than thrown exceptions.
     */
    private static void runConcurrentConsensus( List<Input> inputs, boolean spearman ) throws Exception {
        // For each input, accumulate the set of distinct values observed across all threads. With a healthy cache,
        // every thread sees the same value, so the set has size 1 (or 0 if no thread happened to touch it — which
        // shouldn't occur given each thread iterates ITERATIONS times over inputs).
        Map<Input, ConcurrentHashMap<Double, Boolean>> observed = new HashMap<>();
        for ( Input in : inputs ) {
            observed.put( in, new ConcurrentHashMap<>() );
        }

        ExecutorService pool = Executors.newFixedThreadPool( THREAD_COUNT );
        try {
            CountDownLatch start = new CountDownLatch( 1 );
            AtomicReference<Throwable> firstError = new AtomicReference<>();
            List<Future<?>> futures = new ArrayList<>( THREAD_COUNT );

            for ( int t = 0; t < THREAD_COUNT; t++ ) {
                final int threadId = t;
                futures.add( pool.submit( () -> {
                    try {
                        start.await();
                        Random localRng = new Random( RNG_SEED + threadId * 7919L );
                        for ( int i = 0; i < ITERATIONS; i++ ) {
                            Input in = inputs.get( localRng.nextInt( inputs.size() ) );
                            double got = spearman
                                    ? CorrelationStats.spearmanPvalue( in.correl, in.n )
                                    : CorrelationStats.pvalue( in.correl, in.n );
                            if ( Double.isNaN( got ) ) {
                                firstError.compareAndSet( null,
                                        new AssertionError( "NaN returned for " + in ) );
                                return;
                            }
                            observed.get( in ).putIfAbsent( got, Boolean.TRUE );
                        }
                    } catch ( Throwable th ) {
                        firstError.compareAndSet( null, th );
                    }
                } ) );
            }

            start.countDown();
            for ( Future<?> f : futures ) {
                f.get( 60, TimeUnit.SECONDS );
            }

            Throwable err = firstError.get();
            assertNull( err, () -> "concurrent corruption: "
                    + ( err == null ? "" : err.getClass().getSimpleName() + ": " + err.getMessage() ) );
        } finally {
            pool.shutdownNow();
        }

        // Each input must have exactly one observed value across all threads. Multiple distinct values means
        // different threads disagree about what's cached — corruption.
        for ( Input in : inputs ) {
            Set<Double> seen = observed.get( in ).keySet();
            assertEquals( 1, seen.size(),
                    () -> "threads disagreed on value for " + in + ": observed " + seen );
        }
    }
}
