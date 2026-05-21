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
package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure-mock unit test for {@link DiffExGeneWarmupService}. Asserts the
 * scheduled callable iterates the seed list, tolerates one failing gene, and
 * cleanly no-ops on an empty seed list or with the kill-switch off.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DiffExGeneWarmupServiceTest {

    @Mock
    private GeneService geneService;

    @Mock
    private DifferentialExpressionResultService dearService;

    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private DiffExGeneWarmupService warmupService;

    private final Taxon human = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );

    @BeforeEach
    void setUp() {
        warmupService.setEnabled( true );
        warmupService.setMaxPassMillis( 60_000L );
        when( expressionExperimentService.loadIdsWithCache( ( Filters ) any(), any( Sort.class ) ) )
                .thenReturn( Arrays.asList( 1L, 2L, 3L ) );
        // Sort isn't actually used by the warmer logic beyond being passed through;
        // the call inside the SUT references the service's getSort() helper.
        when( expressionExperimentService.getSort( eq( "id" ), any( Sort.Direction.class ), any( Sort.NullMode.class ) ) )
                .thenReturn( Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
    }

    @Test
    void warmupRunsFindByGeneForEachSeed() {
        warmupService.setSeedSymbolsCsv( "TP53,BRCA1,KRAS" );

        when( geneService.findByOfficialSymbol( "TP53" ) ).thenReturn( Collections.singletonList( geneFor( 100L ) ) );
        when( geneService.findByOfficialSymbol( "BRCA1" ) ).thenReturn( Collections.singletonList( geneFor( 101L ) ) );
        when( geneService.findByOfficialSymbol( "KRAS" ) ).thenReturn( Collections.singletonList( geneFor( 102L ) ) );

        warmupService.warmTopGenes();

        verify( geneService ).findByOfficialSymbol( "TP53" );
        verify( geneService ).findByOfficialSymbol( "BRCA1" );
        verify( geneService ).findByOfficialSymbol( "KRAS" );
        verify( dearService, times( 3 ) ).findByGeneAndExperimentAnalyzedIds(
                any( Gene.class ), anyBoolean(), anyBoolean(),
                any( Collection.class ), anyBoolean(),
                anyMap(), anyMap(), anyMap(),
                anyDouble(), anyBoolean() );
    }

    @Test
    void oneFailingGeneDoesNotAbortTheRest() {
        warmupService.setSeedSymbolsCsv( "TP53,BRCA1,KRAS" );

        Gene tp53 = geneFor( 100L );
        Gene brca1 = geneFor( 101L );
        Gene kras = geneFor( 102L );
        when( geneService.findByOfficialSymbol( "TP53" ) ).thenReturn( Collections.singletonList( tp53 ) );
        when( geneService.findByOfficialSymbol( "BRCA1" ) ).thenReturn( Collections.singletonList( brca1 ) );
        when( geneService.findByOfficialSymbol( "KRAS" ) ).thenReturn( Collections.singletonList( kras ) );

        // BRCA1 throws — TP53 and KRAS must still be warmed.
        when( dearService.findByGeneAndExperimentAnalyzedIds(
                eq( brca1 ), anyBoolean(), anyBoolean(),
                any( Collection.class ), anyBoolean(),
                anyMap(), anyMap(), anyMap(),
                anyDouble(), anyBoolean() ) )
                .thenThrow( new RuntimeException( "boom" ) );

        warmupService.warmTopGenes();

        verify( dearService ).findByGeneAndExperimentAnalyzedIds(
                eq( tp53 ), anyBoolean(), anyBoolean(),
                any( Collection.class ), anyBoolean(),
                anyMap(), anyMap(), anyMap(),
                anyDouble(), anyBoolean() );
        verify( dearService ).findByGeneAndExperimentAnalyzedIds(
                eq( brca1 ), anyBoolean(), anyBoolean(),
                any( Collection.class ), anyBoolean(),
                anyMap(), anyMap(), anyMap(),
                anyDouble(), anyBoolean() );
        verify( dearService ).findByGeneAndExperimentAnalyzedIds(
                eq( kras ), anyBoolean(), anyBoolean(),
                any( Collection.class ), anyBoolean(),
                anyMap(), anyMap(), anyMap(),
                anyDouble(), anyBoolean() );
    }

    @Test
    void emptySeedListIsCleanNoOp() {
        warmupService.setSeedSymbolsCsv( "   " );

        warmupService.warmTopGenes();

        verifyNoInteractions( geneService, dearService );
    }

    @Test
    void disabledKillSwitchSkipsEverything() {
        warmupService.setEnabled( false );
        warmupService.setSeedSymbolsCsv( "TP53" );

        warmupService.warmTopGenes();

        verifyNoInteractions( geneService, dearService, expressionExperimentService );
    }

    @Test
    void unresolvedSymbolIsSkippedButOthersStillWarm() {
        warmupService.setSeedSymbolsCsv( "GHOSTGENE,TP53" );
        when( geneService.findByOfficialSymbol( "GHOSTGENE" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByOfficialSymbol( "TP53" ) ).thenReturn( Collections.singletonList( geneFor( 100L ) ) );

        warmupService.warmTopGenes();

        // exactly one warming call — for TP53. GHOSTGENE never reached DEAR.
        verify( dearService, times( 1 ) ).findByGeneAndExperimentAnalyzedIds(
                any( Gene.class ), anyBoolean(), anyBoolean(),
                any( Collection.class ), anyBoolean(),
                anyMap(), anyMap(), anyMap(),
                anyDouble(), anyBoolean() );
    }

    @Test
    void parseSeedSymbolsTrimsAndIgnoresBlanks() {
        List<String> out = DiffExGeneWarmupService.parseSeedSymbols( " TP53 , , BRCA1,," );
        assertThat( out ).containsExactly( "TP53", "BRCA1" );
    }

    @Test
    void parseSeedSymbolsHandlesNull() {
        assertThat( DiffExGeneWarmupService.parseSeedSymbols( null ) ).isEmpty();
    }

    private Gene geneFor( long id ) {
        Gene g = Gene.Factory.newInstance();
        g.setId( id );
        g.setOfficialSymbol( "G" + id );
        g.setTaxon( human );
        return g;
    }

}
