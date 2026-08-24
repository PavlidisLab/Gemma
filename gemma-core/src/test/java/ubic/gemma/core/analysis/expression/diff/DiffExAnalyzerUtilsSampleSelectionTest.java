package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.experiment.RandomExpressionExperimentUtils.randomExpressionExperiment;

/**
 * Samples marked DE_Exclude and samples whose assays are flagged as outliers must leave the analysis before anything
 * is decided from the sample set. Everything downstream — baseline selection, {@code dropIncompleteFactors}, the
 * analysis-type choice and the degrees-of-freedom guard — reads that set.
 *
 * @author paul
 */
public class DiffExAnalyzerUtilsSampleSelectionTest {

    private static final int NUM_ASSAYS = 8;
    private static final int NUM_PROBES = 20;

    private ExpressionExperiment ee;
    private List<BioAssay> assays;
    private ExperimentalFactor treatment;
    private FactorValue control;
    private FactorValue treated;

    @BeforeEach
    public void setUp() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < NUM_PROBES; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad ) );
        }
        ee = randomExpressionExperiment( taxon, NUM_ASSAYS, ad );
        assays = new ArrayList<>( ee.getBioAssays() );

        treatment = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        treatment.setId( 1L );
        control = FactorValue.Factory.newInstance( treatment );
        control.setId( 1L );
        control.setValue( "control" );
        treated = FactorValue.Factory.newInstance( treatment );
        treated.setId( 2L );
        treated.setValue( "treated" );
        treatment.getFactorValues().add( control );
        treatment.getFactorValues().add( treated );
        for ( int i = 0; i < assays.size(); i++ ) {
            assays.get( i ).getSampleUsed().getFactorValues().add( i < assays.size() / 2 ? control : treated );
        }
    }

    @Test
    public void testAnOrdinarySampleIsAnalyzed() {
        assertThat( ee.getBioAssays() )
                .allSatisfy( ba -> assertThat( DiffExAnalyzerUtils.isAnalyzed( ba.getSampleUsed() ) ).isTrue() );
    }

    @Test
    public void testDeExcludedSampleIsNotAnalyzed() {
        BioMaterial excluded = assays.get( 0 ).getSampleUsed();
        excluded.getFactorValues().add( deExcludeValue() );
        assertThat( DiffExAnalyzerUtils.isAnalyzed( excluded ) ).isFalse();
    }

    @Test
    public void testOutlierSampleIsNotAnalyzed() {
        BioAssay outlier = assays.get( 0 );
        outlier.setIsOutlier( true );
        assertThat( DiffExAnalyzerUtils.isAnalyzed( outlier.getSampleUsed() ) ).isFalse();
    }

    @Test
    public void testMatrixIsUntouchedWhenThereIsNothingToDrop() {
        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee );
        assertThat( DiffExAnalyzerUtils.dropSamplesNotAnalyzed( matrix ) ).isSameAs( matrix );
    }

    @Test
    public void testDropsBothDeExcludedAndOutlierSamples() {
        assays.get( 0 ).getSampleUsed().getFactorValues().add( deExcludeValue() );
        assays.get( 1 ).setIsOutlier( true );
        BioMaterial keptExcluded = assays.get( 0 ).getSampleUsed();
        BioMaterial keptOutlier = assays.get( 1 ).getSampleUsed();

        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee );
        ExpressionDataDoubleMatrix dropped = DiffExAnalyzerUtils.dropSamplesNotAnalyzed( matrix );

        assertThat( dropped.columns() ).isEqualTo( NUM_ASSAYS - 2 );
        assertThat( dropped.getBioMaterials() )
                .doesNotContain( keptExcluded, keptOutlier )
                .hasSize( NUM_ASSAYS - 2 );
        assertThat( dropped.rows() ).isEqualTo( matrix.rows() );
    }

    /**
     * The point of dropping early: a factor whose second level exists only on samples that will not be modelled must
     * not read as analyzable.
     */
    @Test
    public void testCheckValidForLmSeesOnlyTheAnalyzedSamples() {
        // move every "treated" sample out of the analysis, leaving one level with replicates
        List<BioMaterial> analyzed = new ArrayList<>();
        for ( BioAssay ba : assays ) {
            if ( ba.getSampleUsed().getFactorValues().contains( treated ) ) {
                ba.setIsOutlier( true );
            } else {
                analyzed.add( ba.getSampleUsed() );
            }
        }

        assertThat( DifferentialExpressionAnalysisUtil.checkValidForLm( ee, treatment ) ).isTrue();
        assertThat( DifferentialExpressionAnalysisUtil.checkValidForLm( analyzed, treatment ) ).isFalse();
    }

    private FactorValue deExcludeValue() {
        ExperimentalFactor marker = ExperimentalFactor.Factory.newInstance( "collection of material", FactorType.CATEGORICAL );
        marker.setId( 2L );
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( "collection of material" );
        c.setCategoryUri( "http://www.ebi.ac.uk/efo/EFO_0005066" );
        c.setValue( "DE_Exclude" );
        c.setValueUri( FactorValueUtils.DE_EXCLUDE_URI );
        FactorValue fv = FactorValue.Factory.newInstance( marker );
        fv.setId( 3L );
        fv.getCharacteristics().add( Statement.Factory.newInstance( c ) );
        marker.getFactorValues().add( fv );
        return fv;
    }
}
