package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class AbstractAnalyzerTest extends BaseSpringContextTest {

    private Log log = LogFactory.getLog( this.getClass() );

    TTestAnalyzer analyzer = new TTestAnalyzer();

    ExpressionDataMatrix matrix = null;

    Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

    ExpressionExperiment ee = null;

    ExperimentalFactor ef = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ee = this.getTestPersistentCompleteExpressionExperiment();

        // use one experimental factor
        Collection<ExperimentalFactor> efs = ee.getExperimentalDesign().getExperimentalFactors();
        ef = efs.iterator().next();
        Collection<FactorValue> factorValues = ef.getFactorValues();
        Object[] objs = factorValues.toArray();
        FactorValue[] factorValuesAsArray = new FactorValue[objs.length];
        for ( int i = 0; i < objs.length; i++ ) {
            factorValuesAsArray[i] = ( FactorValue ) objs[i];
        }

        Collection<DesignElementDataVector> dedvs = ee.getDesignElementDataVectors();

        QuantitationType quantitationTypeToUse = null;

        Collection<QuantitationType> qts = ee.getQuantitationTypes();
        Collection<BioAssayDimension> dimensions = new HashSet<BioAssayDimension>();
        for ( DesignElementDataVector vector : dedvs ) {
            BioAssayDimension bioAssayDimension = vector.getBioAssayDimension();
            dimensions.add( bioAssayDimension );
        }

        for ( QuantitationType qt : qts ) {
            StandardQuantitationType standardType = qt.getType();
            if ( standardType == StandardQuantitationType.AMOUNT ) quantitationTypeToUse = qt;
            break;
        }

        // TODO use the builder instead
        // ExpressionDataMatrixBuilder matrixBuilder = new ExpressionDataMatrixBuilder( dedvs );
        // matrixBuilder.getIntensity( arrayDesign );

        matrix = new ExpressionDataDoubleMatrix( dedvs, dimensions.iterator().next(), quantitationTypeToUse );

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        Collection<BioAssay> assays = new ArrayList<BioAssay>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bioassays = matrix.getBioAssaysForColumn( i );
            if ( bioassays.size() != 1 )
                throw new RuntimeException( "Invalid number of bioassays.  Expecting 1, got " + bioassays.size() + "." );
            assays.add( bioassays.iterator().next() );
        }

        int i = 0;
        for ( BioAssay assay : assays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 )
                throw new RuntimeException( "Invalid number of biomaterials.  Expecting 1 biomaterial/bioassay, got "
                        + materials.size() + "." );
            // TODO move this - set factor values on bm in test experiment creation.
            for ( BioMaterial m : materials ) {
                Collection<FactorValue> fvs = m.getFactorValues();
                if ( fvs.size() == 0 ) {
                    fvs.add( factorValuesAsArray[i % 2] );
                    m.setFactorValues( fvs );
                }
            }

            biomaterials.addAll( materials );
            i++;
        }
    }
}
