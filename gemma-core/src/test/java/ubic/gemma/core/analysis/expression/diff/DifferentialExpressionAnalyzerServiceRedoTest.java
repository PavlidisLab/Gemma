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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultCache;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
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

    /**
     * A paired analysis reports no result set for its blocking factor, so a redo has to recover it from the
     * protocol description AND put it back in the factors to include; a blocking factor outside
     * {@code factorsToInclude} is dropped by the analyzer and the redo refits unpaired.
     */
    @Test
    public void testRedoRestoresBlockingFactorIntoTheModel() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 10L );
        ee.setShortName( "GSE17183" );
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        ExperimentalFactor subject = factor( 3L, "subject" );
        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();
        design.getExperimentalFactors().addAll( Arrays.asList( treatment, subject ) );
        ee.setExperimentalDesign( design );

        DifferentialExpressionAnalysis old = analysis( 200L, ee, Collections.singletonList( treatment ) );
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setDescription( "# Factors: " + treatment + ", " + subject + "\n"
                + DiffExAnalyzerUtils.BLOCKING_FACTORS_PROTOCOL_PREFIX + subject + "\n"
                + DiffExAnalyzerUtils.ESTIMATE_BLOCKING_CORRELATION_PROTOCOL_LINE + "\n" );
        old.setProtocol( protocol );
        DifferentialExpressionAnalysis redone = analysis( null, ee, Collections.singletonList( treatment ) );

        when( differentialExpressionAnalysisService.canDelete( old ) ).thenReturn( true );
        when( differentialExpressionAnalysisService.thaw( old ) ).thenReturn( old );
        when( differentialExpressionAnalysisService.findByExperiment( ee, true ) )
                .thenReturn( Collections.singletonList( old ) );
        when( differentialExpressionAnalysisService.thaw( anyCollection() ) ).then( returnsFirstArg() );
        when( analysisSelectionAndExecutionService.analyze( eq( ee ), any( DifferentialExpressionAnalysisConfig.class ) ) )
                .thenReturn( Collections.singletonList( redone ) );
        when( helperService.persistStub( any() ) ).then( returnsFirstArg() );

        analyzerService.redoAnalysis( ee, old, new DifferentialExpressionAnalysisConfig() );

        ArgumentCaptor<DifferentialExpressionAnalysisConfig> captor = ArgumentCaptor
                .forClass( DifferentialExpressionAnalysisConfig.class );
        verify( analysisSelectionAndExecutionService ).analyze( eq( ee ), captor.capture() );
        assertThat( captor.getValue().getBlockingFactors() ).containsExactly( subject );
        assertThat( captor.getValue().getFactorsToInclude() ).contains( treatment, subject );
        assertThat( captor.getValue().isEstimateBlockingCorrelation() ).isTrue();
    }

    private DifferentialExpressionAnalysisConfig extend( DifferentialExpressionAnalysis dea ) {
        return ReflectionTestUtils.invokeMethod( analyzerService, "extendConfig", new DifferentialExpressionAnalysisConfig(), dea );
    }

    private static ExpressionExperiment experimentWith( ExperimentalFactor... factors ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 10L );
        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();
        design.getExperimentalFactors().addAll( Arrays.asList( factors ) );
        ee.setExperimentalDesign( design );
        return ee;
    }

    private static DifferentialExpressionAnalysis withProtocol( DifferentialExpressionAnalysis a, String description ) {
        Protocol p = Protocol.Factory.newInstance();
        p.setDescription( description );
        a.setProtocol( p );
        return a;
    }

    /**
     * An analysis with no blocking line redoes unblocked, and a mixed-model flag cannot appear from nowhere.
     */
    @Test
    public void redoOfAnUnblockedAnalysisStaysUnblocked() {
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        ExpressionExperiment ee = experimentWith( treatment, factor( 3L, "subject" ) );
        DifferentialExpressionAnalysis old = withProtocol( analysis( 200L, ee, Collections.singletonList( treatment ) ),
                "# Factors: " + treatment + "\n# No interactions defined.\n" );

        DifferentialExpressionAnalysisConfig config = extend( old );
        assertThat( config.getBlockingFactors() ).isEmpty();
        assertThat( config.isEstimateBlockingCorrelation() ).isFalse();
        assertThat( config.getFactorsToInclude() ).containsExactly( treatment );
    }

    /**
     * An analysis with no protocol at all (older rows) redoes as before.
     */
    @Test
    public void redoOfAnAnalysisWithoutAProtocolStillWorks() {
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        DifferentialExpressionAnalysis old = analysis( 200L, experimentWith( treatment ), Collections.singletonList( treatment ) );
        old.setProtocol( null );

        assertThat( extend( old ).getBlockingFactors() ).isEmpty();
    }

    /**
     * A blocking factor named in the protocol that the design no longer has is left out, not guessed at and
     * not fatal: the redo still runs, on the factors that remain.
     */
    @Test
    public void redoSkipsABlockingFactorTheDesignNoLongerHas() {
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        ExperimentalFactor gone = factor( 9L, "subject" );
        DifferentialExpressionAnalysis old = withProtocol(
                analysis( 200L, experimentWith( treatment ), Collections.singletonList( treatment ) ),
                DiffExAnalyzerUtils.BLOCKING_FACTORS_PROTOCOL_PREFIX + gone + "\n" );

        DifferentialExpressionAnalysisConfig config = extend( old );
        assertThat( config.getBlockingFactors() ).isEmpty();
        assertThat( config.getFactorsToInclude() ).containsExactly( treatment );
    }

    /**
     * Two blocking factors and a name containing the list separator: each is recovered, and neither is
     * confused with a factor whose name is a fragment of it.
     */
    @Test
    public void redoRecoversSeveralBlockingFactorsIncludingACommaInAName() {
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        ExperimentalFactor subject = factor( 3L, "dose, mg" );
        ExperimentalFactor mg = factor( 4L, "mg" );
        ExperimentalFactor batch = factor( 5L, "batch" );
        DifferentialExpressionAnalysis old = withProtocol(
                analysis( 200L, experimentWith( treatment, subject, mg, batch ), Collections.singletonList( treatment ) ),
                DiffExAnalyzerUtils.BLOCKING_FACTORS_PROTOCOL_PREFIX + subject + ", " + batch + "\n" );

        DifferentialExpressionAnalysisConfig config = extend( old );
        assertThat( config.getBlockingFactors() ).containsExactlyInAnyOrder( subject, batch );
        assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( treatment, subject, batch );
    }

    /**
     * A subset analysis has no design of its own; the blocking factor is resolved on the source experiment.
     */
    @Test
    public void redoOfASubsetAnalysisResolvesBlockingFactorsOnTheSourceExperiment() {
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        ExperimentalFactor subject = factor( 3L, "subject" );
        ExpressionExperiment source = experimentWith( treatment, subject );
        ExpressionExperimentSubSet subset = ExpressionExperimentSubSet.Factory.newInstance( "subset", source );
        DifferentialExpressionAnalysis old = withProtocol( analysis( 200L, source, Collections.singletonList( treatment ) ),
                DiffExAnalyzerUtils.BLOCKING_FACTORS_PROTOCOL_PREFIX + subject + "\n" );
        old.setExperimentAnalyzed( subset );

        assertThat( extend( old ).getBlockingFactors() ).containsExactly( subject );
    }

    /**
     * What createProtocolForConfig writes is what the redo parses: the two are kept in step by a shared
     * constant, and this holds the pair together end to end.
     */
    @Test
    public void protocolWrittenForAConfigRoundTripsThroughRedo() {
        ExperimentalFactor treatment = factor( 2L, "treatment" );
        ExperimentalFactor subject = factor( 3L, "subject" );
        DifferentialExpressionAnalysisConfig written = new DifferentialExpressionAnalysisConfig();
        written.addFactorsToInclude( Arrays.asList( treatment, subject ) );
        written.setBlockingFactors( Collections.singletonList( subject ) );
        written.setEstimateBlockingCorrelation( true );
        Protocol protocol = DiffExAnalyzerUtils.createProtocolForConfig( written, Collections.emptyMap() );

        DifferentialExpressionAnalysis old = analysis( 200L, experimentWith( treatment, subject ), Collections.singletonList( treatment ) );
        old.setProtocol( protocol );

        DifferentialExpressionAnalysisConfig read = extend( old );
        assertThat( read.getBlockingFactors() ).containsExactly( subject );
        assertThat( read.isEstimateBlockingCorrelation() ).isTrue();
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
