package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.math3.distribution.*;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Utilities for generating random {@link ExpressionDataDoubleMatrix} following various random distributions.
 */
public class RandomExpressionDataMatrixUtils {

    /**
     * Seed used for generating random matrices.
     */
    private static Long seed;

    public static void setSeed( long seed ) {
        RandomExpressionDataMatrixUtils.seed = seed;
    }

    public static ExpressionDataDoubleMatrix randomCountMatrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new NegativeBinomialDistribution( 6, 0.5 ) );
    }


    public static ExpressionDataDoubleMatrix randomLinearMatrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new LogNormalDistribution( 6.39, 1.18 ) );
    }

    public static ExpressionDataDoubleMatrix randomLog2RatiometricMatrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsRatio( true );
        return randomExpressionMatrix( ee, qt, new NormalDistribution( 0, 0.72 ) );
    }

    public static ExpressionDataDoubleMatrix randomLog2Matrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new TruncatedNormalDistribution( 6.25, 1.46, 0, Double.POSITIVE_INFINITY ) );
    }

    public static ExpressionDataDoubleMatrix randomExpressionMatrix( ExpressionExperiment ee, QuantitationType qt, RealDistribution distribution ) {
        int numSamples = ee.getBioAssays().size();
        if ( numSamples == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one bioassay." );
        }
        int numVectors = ee.getBioAssays().iterator().next().getArrayDesignUsed().getCompositeSequences().size();
        if ( numVectors == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one probe." );
        }
        DenseDoubleMatrix<CompositeSequence, BioMaterial> matrix = new DenseDoubleMatrix<>( randomExpressionMatrix( numVectors, numSamples, distribution ) );
        matrix.setRowNames( new ArrayList<>( ee.getBioAssays().iterator().next().getArrayDesignUsed().getCompositeSequences() ) );
        matrix.setColumnNames( ee.getBioAssays().stream().map( BioAssay::getSampleUsed ).collect( Collectors.toList() ) );
        return new ExpressionDataDoubleMatrix( ee, qt, matrix );
    }

    private static ExpressionDataDoubleMatrix randomExpressionMatrix( ExpressionExperiment ee, QuantitationType qt, IntegerDistribution distribution ) {
        int numSamples = ee.getBioAssays().size();
        if ( numSamples == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one bioassay." );
        }
        int numVectors = ee.getBioAssays().iterator().next().getArrayDesignUsed().getCompositeSequences().size();
        if ( numVectors == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one probe." );
        }
        DenseDoubleMatrix<CompositeSequence, BioMaterial> matrix = new DenseDoubleMatrix<>( randomExpressionMatrix( numVectors, numSamples, distribution ) );
        matrix.setRowNames( new ArrayList<>( ee.getBioAssays().iterator().next().getArrayDesignUsed().getCompositeSequences() ) );
        matrix.setColumnNames( ee.getBioAssays().stream().map( BioAssay::getSampleUsed ).collect( Collectors.toList() ) );
        return new ExpressionDataDoubleMatrix( ee, qt, matrix );
    }

    /**
     * Generate a random raw expression matrix following the given continuous distribution.
     */
    public static double[][] randomExpressionMatrix( int numProbes, int numSamples, RealDistribution distribution ) {
        if ( seed != null ) {
            distribution.reseedRandomGenerator( seed );
        }
        double[][] matrix = new double[numProbes][numSamples];
        double[] samples = distribution.sample( numProbes * numSamples );
        for ( int i = 0; i < numProbes; i++ ) {
            for ( int j = 0; j < numSamples; j++ ) {
                matrix[i][j] = samples[i + numProbes * j];
            }
        }
        return matrix;
    }

    /**
     * Generate a random raw expression matrix following the given discrete distribution.
     */
    public static double[][] randomExpressionMatrix( int numProbes, int numSamples, IntegerDistribution distribution ) {
        if ( seed != null ) {
            distribution.reseedRandomGenerator( seed );
        }
        double[][] matrix = new double[numProbes][numSamples];
        int[] samples = distribution.sample( numProbes * numSamples );
        for ( int i = 0; i < numProbes; i++ ) {
            for ( int j = 0; j < numSamples; j++ ) {
                matrix[i][j] = samples[i + numProbes * j];
            }
        }
        return matrix;
    }

    /**
     * Truncated normal distribution.
     * <p>
     * Only sampling is supported.
     */
    private static class TruncatedNormalDistribution extends AbstractRealDistribution {

        private final NormalDistribution normalDistribution;
        private final double min;
        private final double max;

        public TruncatedNormalDistribution( double mean, double sd, double min, double max ) {
            super( null );
            normalDistribution = new NormalDistribution( mean, sd );
            this.min = min;
            this.max = max;
        }

        @Override
        public double density( double x ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double cumulativeProbability( double x ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getNumericalMean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getNumericalVariance() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getSupportLowerBound() {
            return min;
        }

        @Override
        public double getSupportUpperBound() {
            return max;
        }

        @Override
        public boolean isSupportLowerBoundInclusive() {
            return true;
        }

        @Override
        public boolean isSupportUpperBoundInclusive() {
            return true;
        }

        @Override
        public boolean isSupportConnected() {
            return true;
        }

        @Override
        public void reseedRandomGenerator( long seed ) {
            normalDistribution.reseedRandomGenerator( seed );
        }

        @Override
        public double sample() {
            while ( true ) {
                double s = normalDistribution.sample();
                if ( s >= min && s <= max ) {
                    return s;
                }
            }
        }
    }
}
