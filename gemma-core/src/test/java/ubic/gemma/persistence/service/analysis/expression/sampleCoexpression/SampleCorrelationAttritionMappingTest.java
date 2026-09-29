package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilterConfig;
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilterResult;
import ubic.gemma.core.security.audit.AuditEventPayload;
import ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Pins the shape of the filter attrition recorded on {@code SampleCorrelationAnalysisEvent}.
 * <p>
 * Two things here are worth a test rather than a reading. The stage list is an attrition <em>funnel</em>, so its
 * order is meaning, not presentation -- read out of order the numbers describe a different sequence of filters.
 * And a stage that did not run still reports a row count: without that, "this filter removed nothing" and "this
 * filter was skipped" are the same JSON, which is exactly the confusion the {@code applied} flag exists to stop.
 */
public class SampleCorrelationAttritionMappingTest {

    @Test
    public void attritionCarriesEveryStageInTheOrderTheFilterAppliesThem() {
        ExpressionExperimentFilterResult result = new ExpressionExperimentFilterResult();
        result.setStartingRows( 1000 );
        result.setStartingColumns( 12 );
        result.setNoSequencesFilterApplied( true );
        result.setAfterNoSequencesFilter( 980 );
        result.setAffyControlsFilterApplied( false );
        result.setAfterAffyControlsFilter( 980 );
        result.setOutliersFilterApplied( false );
        result.setAfterOutliersFilter( 980 );
        result.setColumnsAfterOutliersFilter( 12 );
        result.setMinPresentFilterApplied( false );
        result.setAfterMinPresentFilter( 980 );
        result.setZeroVarianceFilterApplied( true );
        result.setAfterZeroVarianceFilter( 950 );
        result.setLowExpressionFilterApplied( true );
        result.setAfterLowExpressionFilter( 800 );
        result.setLowVarianceFilterApplied( true );
        result.setAfterLowVarianceFilter( 700 );
        result.setMaxDesignElementsFilterApplied( false );
        result.setAfterMaxDesignElementsFilter( 700 );
        result.setFinalRows( 700 );
        result.setFinalColumns( 12 );

        SampleCorrelationAnalysisPayload payload = SampleCoexpressionAnalysisServiceImpl
                .toAttritionPayload( SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ), result );

        assertThat( payload.stages() )
                .extracting( SampleCorrelationAnalysisPayload.FilterStage::filter,
                        SampleCorrelationAnalysisPayload.FilterStage::applied,
                        SampleCorrelationAnalysisPayload.FilterStage::rowsAfter )
                .containsExactly(
                        tuple( "noSequences", true, 980 ),
                        tuple( "affyControls", false, 980 ),
                        tuple( "outliers", false, 980 ),
                        tuple( "minPresent", false, 980 ),
                        tuple( "zeroVariance", true, 950 ),
                        tuple( "lowExpression", true, 800 ),
                        tuple( "lowVariance", true, 700 ),
                        tuple( "maxDesignElements", false, 700 ) );
        assertThat( payload.startingRows() ).isEqualTo( 1000 );
        assertThat( payload.finalRows() ).isEqualTo( 700 );
        assertThat( payload.startingColumns() ).isEqualTo( 12 );
        assertThat( payload.finalColumns() ).isEqualTo( 12 );
    }

    /**
     * 🛑 The two things a future "why do these two matrices disagree" has to be able to answer, neither of
     * which can be recovered from the stored matrix: which source the data came from, and what cap was in
     * force when it was filtered.
     * <p>
     * {@code unmasked-rebuild} reprocesses the raw vectors, quantile normalization included, against whatever
     * rows that rebuild covered; {@code stored-vectors} filters data normalized at some earlier time against
     * whatever rows existed then. The corpus will hold both — 2,591 experiments were recomputed by the
     * current method in September 2026 — and nothing on {@code SAMPLE_COEXPRESSION_MATRIX} says which is
     * which. Same lesson as {@code experimentSampleCount}: computable at write time, unrecoverable after.
     */
    @Test
    public void thePayloadRecordsHowTheMatrixWasBuilt() {
        ExpressionExperimentFilterResult result = new ExpressionExperimentFilterResult();
        result.setStartingRows( 34330 );
        result.setFinalRows( 2657 );

        SampleCorrelationAnalysisPayload rebuilt = SampleCoexpressionAnalysisServiceImpl.toAttritionPayload(
                SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ), result, "unmasked-rebuild" );
        assertThat( rebuilt.dataSource() ).isEqualTo( "unmasked-rebuild" );
        assertThat( rebuilt.startingRows() )
                .as( "the width the rebuild covered, which is what its normalization was computed over" )
                .isEqualTo( 34330 );
        assertThat( rebuilt.config().maxDesignElements() )
                .as( "the cap in force, without which the funnel's last rung cannot be read" )
                .isEqualTo( 15000 );

        SampleCorrelationAnalysisPayload old = SampleCoexpressionAnalysisServiceImpl.toAttritionPayload(
                SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ), result );
        assertThat( old.dataSource() )
                .as( "null is 'not recorded', never 'stored-vectors'" )
                .isNull();
    }

    /**
     * 🛑 The cap belongs to the correlation matrix and to nothing else.
     * <p>
     * Filtering early is bad for differential expression — it removes the rows you were looking for — so that
     * path stays generous, and the default config leaves the cap off. The correlation matrix asks a different
     * question, one a few thousand variable probes answer as well as thirty thousand do (Paul, 2026-09-17).
     * Two configs, two answers, and the asymmetry is the point: anyone tempted to unify them should fail here
     * first.
     */
    @Test
    public void onlyTheCorrelationMatrixCapsTheDesignElements() {
        assertThat( SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ).getMaxDesignElements() )
                .as( "the correlation matrix caps" )
                .isEqualTo( 15000 );
        assertThat( new ExpressionExperimentFilterConfig().getMaxDesignElements() )
                .as( "everything else, differential expression included, does not" )
                .isZero();
    }

    /**
     * 🛑 The cap is a stage in the funnel, so it has to report like one. The correlation matrix is capped at
     * the most variable design elements and the differential-expression path deliberately is not; without a
     * row of its own here, a run that dropped 20,000 probes to reach the ceiling would look identical in the
     * audit trail to one that never came near it.
     */
    @Test
    public void theDesignElementCapIsItsOwnStageInTheFunnel() {
        ExpressionExperimentFilterResult result = new ExpressionExperimentFilterResult();
        result.setStartingRows( 40000 );
        result.setLowVarianceFilterApplied( true );
        result.setAfterLowVarianceFilter( 22000 );
        result.setMaxDesignElementsFilterApplied( true );
        result.setAfterMaxDesignElementsFilter( 15000 );
        result.setFinalRows( 15000 );

        SampleCorrelationAnalysisPayload payload = SampleCoexpressionAnalysisServiceImpl
                .toAttritionPayload( SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ), result );

        assertThat( payload.stages() )
                .extracting( SampleCorrelationAnalysisPayload.FilterStage::filter,
                        SampleCorrelationAnalysisPayload.FilterStage::applied,
                        SampleCorrelationAnalysisPayload.FilterStage::rowsAfter )
                .endsWith( tuple( "lowVariance", true, 22000 ),
                        tuple( "maxDesignElements", true, 15000 ) );
        assertThat( payload.finalRows() ).isEqualTo( 15000 );
    }

    /**
     * Only the outlier stage can drop samples, and the correlation-matrix run deliberately does not mask them --
     * so a reader has to be able to see that the columns never moved.
     */
    @Test
    public void onlyTheOutlierStageReportsAColumnCount() {
        ExpressionExperimentFilterResult result = new ExpressionExperimentFilterResult();
        result.setColumnsAfterOutliersFilter( 9 );

        SampleCorrelationAnalysisPayload payload = SampleCoexpressionAnalysisServiceImpl
                .toAttritionPayload( SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ), result );

        List<SampleCorrelationAnalysisPayload.FilterStage> stages = payload.stages();
        assertThat( stages ).filteredOn( s -> s.columnsAfter() != null )
                .extracting( SampleCorrelationAnalysisPayload.FilterStage::filter )
                .containsExactly( "outliers" );
        assertThat( stages ).filteredOn( s -> "outliers".equals( s.filter() ) )
                .singleElement()
                .extracting( SampleCorrelationAnalysisPayload.FilterStage::columnsAfter )
                .isEqualTo( 9 );
    }

    /**
     * The configuration travels with the counts: the sample-correlation filter is not the one the "filtered"
     * data download runs, so the numbers are not interpretable on their own.
     */
    @Test
    public void configurationTravelsWithTheCounts() {
        SampleCorrelationAnalysisPayload payload = SampleCoexpressionAnalysisServiceImpl.toAttritionPayload(
                SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( false ),
                new ExpressionExperimentFilterResult() );

        assertThat( payload.config() ).isNotNull();
        assertThat( payload.config().requireSequences() ).isFalse();
        assertThat( payload.config().maskOutliers() ).isFalse();
        assertThat( payload.config().ignoreMinimumSamplesThreshold() ).isTrue();
        assertThat( payload.config().ignoreMinimumDesignElementsThreshold() ).isTrue();
        assertThat( payload.config().lowExpressionCut() )
                .isEqualTo( ExpressionExperimentFilterConfig.DEFAULT_LOW_EXPRESSION_CUT );
    }

    /**
     * The aspect writes the payload through the polymorphic {@code AuditEventPayload} type, so the stored JSON
     * carries a {@code @type} discriminator and a reader has to register the subtype to resolve it. Proving the
     * round-trip here means a rename of the record cannot silently strand every row already written under the
     * old name.
     */
    @Test
    public void payloadRoundTripsThroughTheAuditEventPayloadType() throws Exception {
        SampleCorrelationAnalysisPayload payload = SampleCoexpressionAnalysisServiceImpl.toAttritionPayload(
                SampleCoexpressionAnalysisServiceImpl.cormatFilterConfig( true ),
                new ExpressionExperimentFilterResult() );

        ObjectMapper writer = new ObjectMapper();
        String json = writer.writeValueAsString( ( AuditEventPayload ) payload );
        assertThat( json ).contains( "\"@type\":\"SampleCorrelationAnalysisPayload\"" );

        ObjectMapper reader = new ObjectMapper();
        reader.registerSubtypes( SampleCorrelationAnalysisPayload.class );
        assertThat( reader.readValue( json, AuditEventPayload.class ) ).isEqualTo( payload );
    }
}
