package ubic.gemma.core.analysis.preprocess.filter;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter that removes outliers from expression data by masking them with {@link Double#NaN}.
 * <p>
 * This is sensitive to the multi-assay case such that it will only mask design elements that belong to the assay(s)
 * marked as outliers.
 *
 * @author poirigui
 */
@CommonsLog
public class OutliersFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) {
        Set<BioAssay> outlierAssays = new HashSet<>();
        for ( int j = 0; j < dataMatrix.columns(); j++ ) {
            for ( BioAssay ba : dataMatrix.getBioAssaysForColumn( j ) ) {
                if ( ba.getIsOutlier() ) {
                    outlierAssays.add( ba );
                }
            }
        }
        if ( outlierAssays.isEmpty() ) {
            log.info( "There are no outliers to filter, skipping." );
            return dataMatrix;
        }
        log.info( "There are " + outlierAssays.size() + " outlier assays; masking them out..." );

        DoubleMatrix<CompositeSequence, BioMaterial> maskedMatrix = dataMatrix.getMatrix().copy();

        Set<BioAssayDimension> dimensionWithOutliers = dataMatrix.getBioAssayDimensions().stream()
                .filter( bad -> CollectionUtils.containsAny( bad.getBioAssays(), outlierAssays ) )
                .collect( Collectors.toSet() );

        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            CompositeSequence de = dataMatrix.getDesignElementForRow( i );
            BioAssayDimension bad = dataMatrix.getBioAssayDimension( de );
            if ( dimensionWithOutliers.contains( bad ) ) {
                for ( BioAssay outlier : outlierAssays ) {
                    int j = dataMatrix.getColumnIndex( outlier );
                    maskedMatrix.set( i, j, Double.NaN );
                }
            }
        }

        return new ExpressionDataDoubleMatrix( dataMatrix, maskedMatrix );
    }

    @Override
    public String toString() {
        return "OutliersFilter";
    }
}
