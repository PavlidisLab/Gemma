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
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
}
