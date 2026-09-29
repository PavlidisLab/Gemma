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
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultCache;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Deleting an analysis waits for the archive write that saving it scheduled.
 * <p>
 * That write reads the analysis in its own transaction. Left running, it loaded the analysis after the delete had
 * committed -- its MySQL snapshot predated the delete -- and put it back in the second-level cache, so
 * {@code load(id)} returned an analysis with no row. {@link DifferentialExpressionAnalysisReplacementTest} failed
 * that way in about one run in four; a repeated version of it found the stale cache entry in 7 of 25 repetitions.
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DifferentialExpressionAnalyzerServiceArchiveWriteTest {

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
    private final List<DifferentialExpressionAnalysis> saved = new ArrayList<>();

    private ExpressionExperiment ee;
    private ExperimentalFactor treatment;

    @BeforeEach
    public void setUp() throws Exception {
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 10L );
        ee.setShortName( "GSE1" );
        treatment = ExperimentalFactor.Factory.newInstance();
        treatment.setId( 1L );
        treatment.setName( "treatment" );

        AtomicLong nextId = new AtomicLong( 100L );
        when( helperService.persistStub( any() ) ).then( invocation -> {
            DifferentialExpressionAnalysis a = invocation.getArgument( 0 );
            a.setId( nextId.getAndIncrement() );
            saved.add( a );
            calls.add( "save " + a.getId() );
            return a;
        } );
        when( differentialExpressionAnalysisService.findByExperiment( ee, true ) ).then( invocation -> new ArrayList<>( saved ) );
        when( differentialExpressionAnalysisService.thaw( any( DifferentialExpressionAnalysis.class ) ) ).then( returnsFirstArg() );
        when( differentialExpressionAnalysisService.thaw( anyCollection() ) ).then( returnsFirstArg() );
        doAnswer( invocation -> {
            DifferentialExpressionAnalysis a = invocation.getArgument( 0 );
            saved.remove( a );
            calls.add( "delete " + a.getId() );
            return null;
        } ).when( differentialExpressionAnalysisService ).remove( any( DifferentialExpressionAnalysis.class ) );
        // the write never finishes on its own: only the delete's wait completes it
        when( expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileAsync( any(), anyBoolean() ) ).then( invocation -> {
            DifferentialExpressionAnalysis a = invocation.getArgument( 0 );
            @SuppressWarnings("unchecked")
            Future<Path> write = mock( Future.class );
            when( write.get( anyLong(), any( TimeUnit.class ) ) ).then( i -> {
                calls.add( "archive written " + a.getId() );
                return null;
            } );
            return write;
        } );
        doNothing().when( analyzerService ).deleteStatistics( any(), any() );
    }

    @Test
    public void testTheReplacedAnalysisIsDeletedOnlyAfterItsArchiveWriteFinishes() {
        persist();
        persist();

        assertThat( calls ).containsExactly( "save 100", "archive written 100", "delete 100", "save 101" );
    }

    private void persist() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singletonList( treatment ) );
        DifferentialExpressionAnalysis analysis = DifferentialExpressionAnalysis.Factory.newInstance();
        analysis.setExperimentAnalyzed( ee );
        ExpressionAnalysisResultSet rs = ExpressionAnalysisResultSet.Factory.newInstance();
        rs.setAnalysis( analysis );
        rs.getExperimentalFactors().add( treatment );
        analysis.getResultSets().add( rs );
        analyzerService.persistAnalysis( ee, analysis, config );
    }
}
