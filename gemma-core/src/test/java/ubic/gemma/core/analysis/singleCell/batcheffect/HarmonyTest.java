package ubic.gemma.core.analysis.singleCell.batcheffect;

import org.junit.Test;
import org.rosuda.REngine.REngineException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.datastructure.matrix.SingleCellDesignMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.r.REngineFactory;
import ubic.gemma.core.util.r.StandaloneRConnection;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.visualization.RandomExperimentalDesignUtils;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ContextConfiguration
public class HarmonyTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import(SettingsConfig.class)
    static class CC {

        @Bean
        public REngineFactory rEngineFactory( @Value("${r.exe}") Path rExe ) {
            return () -> new StandaloneRConnection( rExe );
        }
    }

    @Autowired
    private REngineFactory rEngineFactory;

    @Test
    public void test() throws REngineException {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial sample = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, sample );
            sample.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors( ee, ad, qt );
        SingleCellExpressionDataMatrix<?> dataMatrix = SingleCellExpressionDataMatrix.getMatrix( vectors );
        ExperimentalDesign design = RandomExperimentalDesignUtils.randomExperimentalDesign( ee, 4 );
        SingleCellDesignMatrix singleCellDesignMatrix = SingleCellDesignMatrix.from( dataMatrix.getSingleCellDimension(), design, Collections.emptyList() );
        Harmony h = new Harmony( rEngineFactory );
        h.perform( dataMatrix, singleCellDesignMatrix );
    }
}