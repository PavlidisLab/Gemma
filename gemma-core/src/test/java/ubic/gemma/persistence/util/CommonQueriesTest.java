/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.util;

import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Native-SQL coverage gap from NATIVE_SQL_COVERAGE_AUDIT_2026_05_25.md
 * Tier 1.
 *
 * <p>These static helpers wrap three GENE2CS-table native queries that are
 * load-bearing across the vector-retrieval pipeline (CachedProcessedExpressionDataVectorService,
 * DifferentialExpressionResultDao). They previously had zero direct test
 * coverage.</p>
 *
 * <p>Scope here: the empty-input fast-paths. Each method short-circuits to
 * an empty collection without touching the session when either input is
 * empty. That's a real correctness invariant — callers (cached vector
 * service, DEA result fetch) depend on it to avoid superfluous queries on
 * fresh / empty experiments.</p>
 *
 * <p>TODO: full-fixture happy-path tests that exercise the GENE2CS SQL
 * end-to-end need a TableMaintenanceUtilImpl.updateGene2CsEntries seed
 * (precedent in DiffExMetaAnalyzerServiceTest). Add when next touching
 * either getCs2GeneIdMapForGenes or getCs2GeneMapForProbes.</p>
 */
public class CommonQueriesTest {

    @Test
    public void testGetCs2GeneIdMapForGenesEmptyGenesShortCircuits() {
        Session session = mock( Session.class );
        assertThat( CommonQueries.getCs2GeneIdMapForGenes( Collections.emptyList(),
                Collections.singletonList( 1L ), session ) ).isEmpty();
        verifyNoInteractions( session );
    }

    @Test
    public void testGetCs2GeneIdMapForGenesEmptyArrayDesignsShortCircuits() {
        Session session = mock( Session.class );
        assertThat( CommonQueries.getCs2GeneIdMapForGenes( Collections.singletonList( 42L ),
                Collections.emptyList(), session ) ).isEmpty();
        verifyNoInteractions( session );
    }

    @Test
    public void testGetCs2GeneMapForProbesEmptyShortCircuits() {
        Session session = mock( Session.class );
        assertThat( CommonQueries.getCs2GeneMapForProbes( Collections.emptyList(), session ) ).isEmpty();
        verifyNoInteractions( session );
    }

    @Test
    public void testFilterProbesByPlatformEmptyProbesShortCircuits() {
        Session session = mock( Session.class );
        assertThat( CommonQueries.filterProbesByPlatform( Collections.emptyList(),
                Collections.singletonList( 1L ), session ) ).isEmpty();
        verifyNoInteractions( session );
    }

    @Test
    public void testFilterProbesByPlatformEmptyArrayDesignsShortCircuits() {
        Session session = mock( Session.class );
        assertThat( CommonQueries.filterProbesByPlatform( Collections.singletonList( 42L ),
                Collections.emptyList(), session ) ).isEmpty();
        verifyNoInteractions( session );
    }

    /*
     * getExperiment is the shared BioAssaySet -> ExpressionExperiment resolver; the vector service used
     * to carry its own copy. Its callers reach it through associations mapped against the abstract
     * BioAssaySet, so what arrives is a BioAssaySet proxy: an instance of neither concrete subclass,
     * which fell through to "Couldn't handle a ...$HibernateProxy".
     */

    private static ExpressionExperiment ee( Long id ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( id );
        ee.setShortName( "GSE" + id );
        return ee;
    }

    /**
     * See {@code ExpressionDataFileUtilsTest.LazyProxy} — a real class rather than a Mockito mock,
     * because {@code Hibernate.unproxy} goes through {@code asHibernateProxy()} (a default method
     * returning {@code this}) which a mock stubs to {@code null}, letting an unresolved proxy through
     * and passing against the unfixed code.
     */
    private static class LazyProxy extends BioAssaySet implements HibernateProxy {

        private final LazyInitializer li;

        private LazyProxy( BioAssaySet target ) {
            this.li = mock( LazyInitializer.class );
            when( li.getImplementation() ).thenReturn( target );
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return li;
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public boolean equals( Object obj ) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode( this );
        }

        @Override
        public Set<BioAssay> getBioAssays() {
            throw new UnsupportedOperationException( "Nothing should reach through this stand-in." );
        }

        @Override
        public void setBioAssays( Set<BioAssay> bioAssays ) {
            throw new UnsupportedOperationException( "Nothing should reach through this stand-in." );
        }
    }

    @Test
    public void testGetExperimentOfInitializedExperiment() {
        ExpressionExperiment ee = ee( 1L );
        assertThat( CommonQueries.getExperiment( ee ) ).isSameAs( ee );
    }

    @Test
    public void testGetExperimentOfLazyProxiedExperiment() {
        ExpressionExperiment ee = ee( 1L );
        BioAssaySet proxy = new LazyProxy( ee );
        // guard: the proxy really is opaque, otherwise this test proves nothing
        assertThat( proxy ).isNotInstanceOf( ExpressionExperiment.class );

        assertThat( CommonQueries.getExperiment( proxy ) ).isSameAs( ee );
    }

    @Test
    public void testGetExperimentOfInitializedSubsetReturnsItsSource() {
        ExpressionExperiment source = ee( 1L );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 7L );
        subset.setSourceExperiment( source );

        assertThat( CommonQueries.getExperiment( subset ) ).isSameAs( source );
    }

    @Test
    public void testGetExperimentOfLazyProxiedSubsetReturnsItsSource() {
        ExpressionExperiment source = ee( 1L );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 7L );
        subset.setSourceExperiment( source );
        BioAssaySet proxy = new LazyProxy( subset );
        assertThat( proxy ).isNotInstanceOf( ExpressionExperimentSubSet.class );

        assertThat( CommonQueries.getExperiment( proxy ) ).isSameAs( source );
    }
}
