package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.BeanUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for generating random bulk vectors.
 * @see RandomExpressionDataMatrixUtils
 * @see RandomSingleCellDataUtils
 */
public class RandomBulkDataUtils {

    public static void setSeed( int seed ) {
        RandomDataUtils.setSeed( seed );
    }

    public static <T extends BulkExpressionDataVector> Collection<T> randomBulkVectors( ExpressionExperiment expressionExperiment, ArrayDesign ad, QuantitationType qt, Class<T> vectorClass ) {
        BioAssayDimension bad = new BioAssayDimension();
        expressionExperiment.getBioAssays()
                .stream().sorted( Comparator.comparing( BioAssay::getName ) )
                .forEach( bad.getBioAssays()::add );
        return randomBulkVectors( expressionExperiment, bad, ad, qt, vectorClass );
    }

    /**
     * Generate bulk vectors for a collection of subsets.
     */
    public static <T extends BulkExpressionDataVector> Collection<T> randomBulkVectors( ExpressionExperiment ee, Collection<ExpressionExperimentSubSet> subsets, ArrayDesign ad, QuantitationType qt, Class<T> vectorClass ) {
        BioAssayDimension bad = new BioAssayDimension();
        subsets.stream()
                .sorted( Comparator.comparing( ExpressionExperimentSubSet::getName ) )
                .forEach( s -> {
                    s.getBioAssays()
                            .stream().sorted( Comparator.comparing( BioAssay::getName ) )
                            .forEach( bad.getBioAssays()::add );
                } );
        return randomBulkVectors( ee, bad, ad, qt, vectorClass );
    }

    public static <T extends BulkExpressionDataVector> Collection<T> randomBulkVectors( ExpressionExperiment ee, BioAssayDimension bad, ArrayDesign ad, QuantitationType qt, Class<T> vectorClass ) {
        Set<T> vectors = new HashSet<>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            T vector = BeanUtils.instantiate( vectorClass );
            vector.setExpressionExperiment( ee );
            vector.setDesignElement( cs );
            vector.setBioAssayDimension( bad );
            vector.setQuantitationType( qt );
            switch ( qt.getRepresentation() ) {
                case FLOAT:
                    vector.setDataAsFloats( RandomDataUtils.sampleFloats( qt, bad.getBioAssays().size() ) );
                    break;
                case DOUBLE:
                    vector.setDataAsDoubles( RandomDataUtils.sampleDoubles( qt, bad.getBioAssays().size() ) );
                    break;
                case INT:
                    vector.setDataAsInts( RandomDataUtils.sampleInts( qt, bad.getBioAssays().size() ) );
                    break;
                case LONG:
                    vector.setDataAsLongs( RandomDataUtils.sampleLongs( qt, bad.getBioAssays().size() ) );
                    break;
                default:
                    throw new UnsupportedOperationException( qt.getRepresentation() + " is not supported for sampling bulk vectors." );
            }
            vectors.add( vector );
        }
        return vectors;
    }
}
