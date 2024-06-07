package ubic.gemma.core.analysis.expression.diff;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LinearModelAnalyzerUtils {
    public static void populateFactorValuesFromBASet( BioAssaySet ee, ExperimentalFactor f,
            Collection<FactorValue> fvs ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            for ( FactorValue fv : bm.getFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( f ) ) {
                    fvs.add( fv );
                }
            }
        }
    }

    /**
     * Convert the data into a string-keyed matrix. Assumes that the row names of the designMatrix
     * are concordant with the column names of the namedMatrix
     */
    public static DoubleMatrix<String, String> makeDataMatrix( ObjectMatrix<String, String, Object> designMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {

        DoubleMatrix<String, String> sNamedMatrix = new DenseDoubleMatrix<>( namedMatrix.asArray() );
        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            sNamedMatrix.addRowName( namedMatrix.getRowName( i ).getId().toString() );
        }
        sNamedMatrix.setColumnNames( designMatrix.getRowNames() );
        return sNamedMatrix;
    }

    /**
     * This bioAssayDimension shouldn't get persisted; it is only for dealing with subset diff ex. analyses.
     *
     * @param  columnsToUse columns to use
     * @return bio assay dimension
     */
    public static BioAssayDimension createBADMap( List<BioMaterial> columnsToUse ) {
        /*
         * Indices of the biomaterials in the original matrix.
         */
        List<BioAssay> bioAssays = new ArrayList<>();
        for ( BioMaterial bm : columnsToUse ) {
            bioAssays.add( bm.getBioAssaysUsedIn().iterator().next() );
        }

        /*
         * fix the upper level column name maps.
         */
        BioAssayDimension reorderedDim = BioAssayDimension.Factory.newInstance();
        reorderedDim.setBioAssays( bioAssays );
        reorderedDim.setName( "For analysis" );
        reorderedDim.setDescription( bioAssays.size() + " bioAssays" );

        return reorderedDim;
    }
}
