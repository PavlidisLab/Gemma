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
                        tuple( "lowVariance", true, 700 ) );
        assertThat( payload.startingRows() ).isEqualTo( 1000 );
        assertThat( payload.finalRows() ).isEqualTo( 700 );
        assertThat( payload.startingColumns() ).isEqualTo( 12 );
        assertThat( payload.finalColumns() ).isEqualTo( 12 );
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
