package ubic.gemma.core.analysis.expression.diff;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * This test covers subset analysis with pre-existing EE subset structure.
 */
@ContextConfiguration
public class SubsettedAnalysis4Test extends BaseTest {

    @Configuration
    @TestComponent
    static class SubsettedAnalysis4TestContextConfiguration {

        @Bean
        public DiffExAnalyzer diffExAnalyzer() {
            return new LinearModelAnalyzer();
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock();
        }

        @Bean
        public AsyncTaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }
    }

    @Autowired
    private DiffExAnalyzer analyzer;

    @Test
    public void testSingleCellSubSetAnalysis() {
        ArrayDesign ad = new ArrayDesign();

        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            cs.setId( ( long ) i );
            ad.getCompositeSequences().add( cs );
        }

        ExpressionExperiment ee = new ExpressionExperiment();

        // 4 samples
        BioMaterial[] sourceBms = new BioMaterial[4];
        BioAssay[] sourceBas = new BioAssay[4];

        // simple case/control treatment
        ExperimentalFactor treatmentFactor = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        treatmentFactor.setId( 1L );
        FactorValue caseFv = FactorValue.Factory.newInstance( treatmentFactor, Characteristic.Factory.newInstance( Categories.TREATMENT, "case", null ) );
        caseFv.setId( 1L );
        FactorValue controlFv = FactorValue.Factory.newInstance( treatmentFactor, Characteristic.Factory.newInstance( Categories.TREATMENT, "control", null ) );
        controlFv.setId( 2L );
        treatmentFactor.getFactorValues().addAll( Arrays.asList( caseFv, controlFv ) );

        // 4 cell types
        ExperimentalFactor cellTypeFactor = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
        cellTypeFactor.setId( 2L );
        FactorValue[] fvs = {
                FactorValue.Factory.newInstance( cellTypeFactor, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct1", null ) ),
                FactorValue.Factory.newInstance( cellTypeFactor, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct2", null ) ),
                FactorValue.Factory.newInstance( cellTypeFactor, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct3", null ) ),
                FactorValue.Factory.newInstance( cellTypeFactor, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct4", null ) )
        };
        for ( int i = 0; i < 4; i++ ) {
            fvs[i].setId( 2 + ( long ) i );
        }
        cellTypeFactor.getFactorValues().addAll( Arrays.asList( fvs ) );

        Map<FactorValue, ExpressionExperimentSubSet> map = new HashMap<>();
        for ( FactorValue fv : fvs ) {
            ExpressionExperimentSubSet subset = ExpressionExperimentSubSet.Factory.newInstance( "Subset for " + FactorValueUtils.getSummaryString( fv ), ee );
            map.put( fv, subset );
        }

        // 4 cell type per sample
        BioAssayDimension dimension = new BioAssayDimension();
        for ( int i = 0; i < 4; i++ ) {
            BioMaterial sourceBm = BioMaterial.Factory.newInstance( "bm" + i );
            sourceBm.setId( ( long ) i );
            sourceBms[i] = sourceBm;
            sourceBas[i] = BioAssay.Factory.newInstance( "ba" + i, ad, sourceBms[i] );
            sourceBas[i].setSampleUsed( sourceBm );
            sourceBms[i].getBioAssaysUsedIn().add( sourceBas[i] );
            if ( i % 2 == 0 ) {
                // case
                sourceBms[i].getFactorValues().add( caseFv );
            } else {
                // control
                sourceBms[i].getFactorValues().add( controlFv );
            }
            for ( int j = 0; j < 4; j++ ) {
                FactorValue fv = fvs[j];
                BioMaterial bm = BioMaterial.Factory.newInstance( "b" + i + "_" + j );
                // the 4 first are the source BMs
                bm.setId( ( long ) ( 4 + i * 4 + j ) );
                bm.getFactorValues().add( fv );
                bm.setSourceBioMaterial( sourceBm );
                BioAssay ba = BioAssay.Factory.newInstance( "ba" + i + "_" + j, ad, bm );
                ba.setSampleUsed( bm );
                bm.getBioAssaysUsedIn().add( ba );
                dimension.getBioAssays().add( ba );
                map.get( fv ).getBioAssays().add( ba );
            }
        }
        ee.getBioAssays().addAll( Arrays.asList( sourceBas ) );

        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singletonList( treatmentFactor ) );

        config.setSubsetFactor( cellTypeFactor );
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, map, dmatrix, config );
        assertThat( analyses )
                .hasSize( 4 );
    }
}
