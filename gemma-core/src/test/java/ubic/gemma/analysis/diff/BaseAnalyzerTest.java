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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * A useful test class to be extended by analyzers that need an expression experiment.
 * 
 * @deprecated This class will be removed.
 * @author keshav
 * @version $Id$
 */
public class BaseAnalyzerTest extends BaseSpringContextTest {

    private Log log = LogFactory.getLog( this.getClass() );

    protected ExpressionDataMatrix matrix = null;

    protected Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

    protected ExpressionExperiment ee = null;

    protected Collection<ExperimentalFactor> efs = null;
    protected ExperimentalFactor ef = null;

    protected QuantitationType quantitationTypeToUse = null;

    protected BioAssayDimension bioAssayDimension = null;

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

        efs = ee.getExperimentalDesign().getExperimentalFactors();
        ef = efs.iterator().next();
        Collection<FactorValue> factorValues = ef.getFactorValues();
        Object[] objs = factorValues.toArray();
        FactorValue[] factorValuesAsArray = new FactorValue[objs.length];
        for ( int i = 0; i < objs.length; i++ ) {
            factorValuesAsArray[i] = ( FactorValue ) objs[i];
        }

        Collection<DesignElementDataVector> dedvs = ee.getDesignElementDataVectors();

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

        bioAssayDimension = dimensions.iterator().next();
        matrix = new ExpressionDataDoubleMatrix( dedvs, bioAssayDimension, quantitationTypeToUse );

        biomaterials = AnalyzerHelper.getBioMaterialsForBioAssays( matrix );
    }
}
