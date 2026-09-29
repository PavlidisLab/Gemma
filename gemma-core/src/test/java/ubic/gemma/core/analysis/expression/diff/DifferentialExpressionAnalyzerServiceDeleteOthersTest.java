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
package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultCache;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A fresh run with {@code deleteOtherAnalyses} saves its analyses first, then deletes the experiment's other analyses
 * on the same subset factor.
 * <p>
 * Paul, 2026-09-15: the DEA CLI needs a "delete others" option, and it should not "replace analyses that don't need to
 * be updated". Until then the route was {@code deleteDiffEx} followed by {@code diffExAnalyze}; on GSE90654 the
 * experiment had no analysis between the two calls.
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DifferentialExpressionAnalyzerServiceDeleteOthersTest {

    @Mock
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;
    @Mock
    private DifferentialExpressionAnalyzerAuditService differentialExpressionAnalyzerAuditService;
    @Mock
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Mock
    private ExpressionDataFileService expressionDataFileService;
    @Mock
    private DifferentialExpressionAnalysisHelperService helperService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Mock
    private DifferentialExpressionResultCache differentialExpressionResultCache;

    @Spy
    @InjectMocks
    private DifferentialExpressionAnalyzerServiceImpl analyzerService;

    private final List<String> calls = new ArrayList<>();

    private ExpressionExperiment ee;
    private ExperimentalFactor genotype;
    private ExperimentalFactor cellType;
    private DifferentialExpressionAnalysis onGenotype;
    private DifferentialExpressionAnalysis onBoth;
    private DifferentialExpressionAnalysis inNeurons;
    private DifferentialExpressionAnalysisConfig config;

    @BeforeEach
    public void setUp() {
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 10L );
        ee.setShortName( "GSE90654" );
        genotype = factor( 1L, "genotype" );
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        cellType = factor( 3L, "cell type" );
        onGenotype = analysis( 100L, Collections.singletonList( genotype ), null );
        onBoth = analysis( 200L, Arrays.asList( genotype, treatment ), null );
        inNeurons = analysis( 400L, Collections.singletonList( genotype ), factorValue( 31L, cellType ) );

        when( differentialExpressionAnalysisService.findByExperiment( ee, true ) )
                .thenReturn( Arrays.asList( onGenotype, onBoth, inNeurons ) );
        when( differentialExpressionAnalysisService.canDelete( any( DifferentialExpressionAnalysis.class ) ) ).thenReturn( true );
        when( differentialExpressionAnalysisService.thaw( any( DifferentialExpressionAnalysis.class ) ) ).then( returnsFirstArg() );
        when( differentialExpressionAnalysisService.thaw( anyCollection() ) ).then( returnsFirstArg() );
        when( helperService.persistStub( any() ) ).then( invocation -> {
            DifferentialExpressionAnalysis saved = invocation.getArgument( 0 );
            saved.setId( 300L );
            calls.add( "save " + saved.getId() );
            return saved;
        } );
        doAnswer( invocation -> {
            calls.add( "delete " + invocation.<DifferentialExpressionAnalysis>getArgument( 0 ).getId() );
            return null;
        } ).when( differentialExpressionAnalysisService ).remove( any( DifferentialExpressionAnalysis.class ) );
        // the p-value distribution files sit under the analysis storage path; nothing here should touch the disk
        doNothing().when( analyzerService ).deleteStatistics( any(), any() );

        config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singletonList( genotype ) );
        config.setDeleteOtherAnalyses( true );
    }

    /**
     * Includes the analysis on the same factors, which a run without the option deletes before saving. The subset
     * analysis is on another subset factor, so it stays.
     */
    @Test
    public void testTheOtherAnalysesAreDeletedAfterTheNewOneIsSaved() {
        DifferentialExpressionAnalysis fresh = analysis( null, Collections.singletonList( genotype ), null );
        when( analysisSelectionAndExecutionService.analyze( ee, config ) ).thenReturn( Collections.singletonList( fresh ) );

        Collection<DifferentialExpressionAnalysis> results = analyzerService.runDifferentialExpressionAnalyses( ee, config );

        assertThat( results ).containsExactly( fresh );
        assertThat( calls ).containsExactly( "save 300", "delete 100", "delete 200" );
    }

    @Test
    public void testASubsetRunReplacesOnlyTheAnalysesOfThatFactorsSubsets() {
        config.setSubsetFactor( cellType );
        DifferentialExpressionAnalysis fresh = analysis( null, Collections.singletonList( genotype ), factorValue( 32L, cellType ) );
        when( analysisSelectionAndExecutionService.analyze( ee, config ) ).thenReturn( Collections.singletonList( fresh ) );

        analyzerService.runDifferentialExpressionAnalyses( ee, config );

        assertThat( calls ).containsExactly( "save 300", "delete 400" );
    }

    /**
     * An analysis the run does not replace cannot refuse it.
     */
    @Test
    public void testAnAnalysisOutsideTheRunIsNotChecked() {
        when( differentialExpressionAnalysisService.canDelete( inNeurons ) ).thenReturn( false );
        DifferentialExpressionAnalysis fresh = analysis( null, Collections.singletonList( genotype ), null );
        when( analysisSelectionAndExecutionService.analyze( ee, config ) ).thenReturn( Collections.singletonList( fresh ) );

        analyzerService.runDifferentialExpressionAnalyses( ee, config );

        assertThat( calls ).containsExactly( "save 300", "delete 100", "delete 200" );
    }

    @Test
    public void testNothingIsDeletedWhenTheAnalysisFails() {
        when( analysisSelectionAndExecutionService.analyze( ee, config ) )
                .thenThrow( new IllegalStateException( "design is not valid" ) );

        assertThatThrownBy( () -> analyzerService.runDifferentialExpressionAnalyses( ee, config ) )
                .isInstanceOf( IllegalStateException.class );

        assertThat( calls ).isEmpty();
    }

    @Test
    public void testNothingIsDeletedWhenNoAnalysisIsProduced() {
        when( analysisSelectionAndExecutionService.analyze( ee, config ) ).thenReturn( Collections.emptyList() );

        assertThat( analyzerService.runDifferentialExpressionAnalyses( ee, config ) ).isEmpty();

        assertThat( calls ).isEmpty();
    }

    /**
     * Checked before the analysis runs, so a refusal costs nothing and leaves nothing half-replaced.
     */
    @Test
    public void testAnAnalysisThatCannotBeDeletedRefusesTheRunBeforeItStarts() {
        when( differentialExpressionAnalysisService.canDelete( onBoth ) ).thenReturn( false );

        assertThatThrownBy( () -> analyzerService.runDifferentialExpressionAnalyses( ee, config ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "Cannot delete" );

        verifyNoInteractions( analysisSelectionAndExecutionService );
        assertThat( calls ).isEmpty();
    }

    @Test
    public void testARedoRefusesTheOption() {
        assertThatThrownBy( () -> analyzerService.redoAnalysis( ee, onGenotype, config ) )
                .isInstanceOf( IllegalArgumentException.class );

        verify( helperService, never() ).persistStub( any() );
        assertThat( calls ).isEmpty();
    }

    private static ExperimentalFactor factor( Long id, String name ) {
        ExperimentalFactor f = ExperimentalFactor.Factory.newInstance();
        f.setId( id );
        f.setName( name );
        return f;
    }

    private static FactorValue factorValue( Long id, ExperimentalFactor factor ) {
        FactorValue fv = FactorValue.Factory.newInstance( factor );
        fv.setId( id );
        return fv;
    }

    private DifferentialExpressionAnalysis analysis( @Nullable Long id, Collection<ExperimentalFactor> factors,
            @Nullable FactorValue subsetFactorValue ) {
        DifferentialExpressionAnalysis a = DifferentialExpressionAnalysis.Factory.newInstance();
        a.setId( id );
        a.setExperimentAnalyzed( ee );
        a.setSubsetFactorValue( subsetFactorValue );
        ExpressionAnalysisResultSet rs = ExpressionAnalysisResultSet.Factory.newInstance();
        rs.setAnalysis( a );
        rs.getExperimentalFactors().addAll( factors );
        a.getResultSets().add( rs );
        return a;
    }
}
