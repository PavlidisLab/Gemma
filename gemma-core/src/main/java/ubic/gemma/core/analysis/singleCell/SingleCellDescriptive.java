package ubic.gemma.core.analysis.singleCell;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleDataAsDoubles;

/**
 * Descriptive statistics for single cell data.
 * @see Descriptive
 */
public class SingleCellDescriptive {

    /**
     * Calculate the mean of each assay for a given vector.
     */
    public static double[] mean( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            DoubleArrayList vec = new DoubleArrayList( getSampleDataAsDoubles( vector, i ) );
            d[i] = mean( vec, scaleType );
        }
        return d;
    }

    /**
     * Calculate the mean of a given assay.
     */
    public static double mean( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( "Sample not found in vector" );
        }
        return mean( new DoubleArrayList( getSampleDataAsDoubles( vector, sampleIndex ) ), vector.getQuantitationType().getScale() );
    }

    private static double mean( DoubleArrayList vec, ScaleType scaleType ) {
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                return Descriptive.geometricMean( vec );
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return Descriptive.mean( vec );
            // TODO:
            // case LOG1P:
            //     return Math.log1p( Descriptive.mean( vec.forEach( Math::expm1 ) ) );
            default:
                throw new IllegalArgumentException( "Don't know how to calculate mean for scale type " + scaleType );
        }
    }

    public static double[] sampleVariance( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            DoubleArrayList vec = new DoubleArrayList( getSampleDataAsDoubles( vector, i ) );
            d[i] = sampleVariance( vec, scaleType );
        }
        return d;
    }

    /**
     * Calculate the variance of a given assay.
     */
    public static double sampleVariance( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( "Sample not found in vector" );
        }
        DoubleArrayList vec = new DoubleArrayList( getSampleDataAsDoubles( vector, sampleIndex ) );
        return sampleVariance( vec, vector.getQuantitationType().getScale() );
    }

    private static double sampleVariance( DoubleArrayList vec, ScaleType scaleType ) {
        double mean = Descriptive.mean( vec );
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
                // https://en.wikipedia.org/wiki/Geometric_standard_deviation
                // this is the counterpart of the geometric mean
                double[] arr = vec.elements();
                double var = 0;
                for ( double v : arr ) {
                    var += Math.pow( Math.log( v / mean ), 2 );
                }
                return Math.exp( Math.sqrt( var / arr.length ) );
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return Descriptive.sampleVariance( vec, mean );
            // TODO:
            // case LOG1P:
            //     return Math.log1p( Descriptive.mean( vec.forEach( Math::expm1 ) ) );
            default:
                throw new IllegalArgumentException( "Don't know how to calculate mean for scale type " + scaleType );
        }
    }
}
