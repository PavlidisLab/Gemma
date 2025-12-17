package ubic.gemma.core.analysis.preprocess.filter;

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.HashSet;
import java.util.Set;

public class ExpressionDataFilterUtils {

    /**
     * Obtain the set of samples (columns) with data.
     */
    public static Set<BioMaterial> getSamplesWithData( ExpressionDataDoubleMatrix dataMatrix ) {
        Set<BioMaterial> samplesWithData = new HashSet<>( dataMatrix.columns() );
        for ( int j = 0; j < dataMatrix.columns(); j++ ) {
            for ( int i = 0; i < dataMatrix.rows(); i++ ) {
                if ( !Double.isNaN( dataMatrix.getAsDouble( i, j ) ) ) {
                    samplesWithData.add( dataMatrix.getBioMaterialForColumn( j ) );
                    break;
                }
            }
        }
        return samplesWithData;
    }

    /**
     * Count the number of samples in the given matrix.
     * <p>
     * This takes into account samples that may have been filled with {@link Double#NaN}s, so it is more accurate than
     * just looking up {@link ExpressionDataMatrix#columns()}.
     */
    public static int countSamplesWithData( ExpressionDataDoubleMatrix dataMatrix ) {
        int samplesWithData = 0;
        for ( int j = 0; j < dataMatrix.columns(); j++ ) {
            for ( int i = 0; i < dataMatrix.rows(); i++ ) {
                if ( !Double.isNaN( dataMatrix.getAsDouble( i, j ) ) ) {
                    samplesWithData++;
                    break;
                }
            }
        }
        return samplesWithData;
    }
}
