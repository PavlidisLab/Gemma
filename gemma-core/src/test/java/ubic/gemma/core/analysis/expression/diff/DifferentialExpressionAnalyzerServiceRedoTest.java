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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultCache;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A redo keeps the old replacement rule: only an analysis on the identical factor set is replaced.
 * <p>
 * A redo re-persists each existing analysis in turn. If redoing {@code genotype} + {@code treatment} also replaced
 * the analysis on {@code genotype} alone, it would delete an analysis the same loop has yet to redo. The wider rule
 * (Paul's ruling, 2026-09-13) applies to a fresh run; {@link DifferentialExpressionAnalysisReplacementTest} covers
 * that against the database.
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DifferentialExpressionAnalyzerServiceRedoTest {

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

    @InjectMocks
    private DifferentialExpressionAnalyzerServiceImpl analyzerService;

    @Test
    public void testRedoingAWiderAnalysisDoesNotDeleteANarrowerOne() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 10L );
        ee.setShortName( "GSE107259" );
        ExperimentalFactor genotype = factor( 1L, "genotype" );
        ExperimentalFactor treatment = factor( 2L, "treatment" );

        DifferentialExpressionAnalysis narrower = analysis( 100L, ee, Collections.singletonList( genotype ) );
        DifferentialExpressionAnalysis wider = analysis( 200L, ee, Arrays.asList( genotype, treatment ) );
        DifferentialExpressionAnalysis redone = analysis( null, ee, Arrays.asList( genotype, treatment ) );

        when( differentialExpressionAnalysisService.canDelete( wider ) ).thenReturn( true );
        when( differentialExpressionAnalysisService.thaw( wider ) ).thenReturn( wider );
        // unused unless the rule breaks: lets a wrongful deletion reach remove() rather than stop at a null thaw
        when( differentialExpressionAnalysisService.thaw( narrower ) ).thenReturn( narrower );
        when( differentialExpressionAnalysisService.findByExperiment( ee, true ) )
                .thenReturn( Collections.singletonList( narrower ) );
        when( differentialExpressionAnalysisService.thaw( anyCollection() ) ).then( returnsFirstArg() );
        when( analysisSelectionAndExecutionService.analyze( eq( ee ), any( DifferentialExpressionAnalysisConfig.class ) ) )
                .thenReturn( Collections.singletonList( redone ) );
        when( helperService.persistStub( any() ) ).then( returnsFirstArg() );

        analyzerService.redoAnalysis( ee, wider, new DifferentialExpressionAnalysisConfig() );

        verify( differentialExpressionAnalysisService, never() ).remove( narrower );
    }

    private static ExperimentalFactor factor( Long id, String name ) {
        ExperimentalFactor f = ExperimentalFactor.Factory.newInstance();
        f.setId( id );
        f.setName( name );
        return f;
    }

    private static DifferentialExpressionAnalysis analysis( Long id, ExpressionExperiment ee,
            Collection<ExperimentalFactor> factors ) {
        DifferentialExpressionAnalysis a = DifferentialExpressionAnalysis.Factory.newInstance();
        a.setId( id );
        a.setExperimentAnalyzed( ee );
        ExpressionAnalysisResultSet rs = ExpressionAnalysisResultSet.Factory.newInstance();
        rs.setAnalysis( a );
        rs.getExperimentalFactors().addAll( factors );
        a.getResultSets().add( rs );
        return a;
    }
}
