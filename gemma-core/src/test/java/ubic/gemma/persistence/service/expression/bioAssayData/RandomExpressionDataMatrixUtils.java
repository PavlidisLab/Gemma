package ubic.gemma.persistence.service.expression.bioAssayData;

import cern.jet.random.engine.MersenneTwister;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;
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
        QuantitationType qt = new QuantitationTypeImpl();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new NegativeBinomialDistribution( 6, 0.5 ) );
    }


    public static ExpressionDataDoubleMatrix randomLinearMatrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationTypeImpl();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new LogNormalDistribution( 6.39, 1.18 ) );
    }

    public static ExpressionDataDoubleMatrix randomLog2RatiometricMatrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationTypeImpl();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsRatio( true );
        return randomExpressionMatrix( ee, qt, new NormalDistribution( 0, 0.72 ) );
    }

    public static ExpressionDataDoubleMatrix randomLog2Matrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationTypeImpl();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new NormalDistribution( 6.25, 1.46 ) );
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

    private static double[][] randomExpressionMatrix( int rows, int cols, RealDistribution distribution ) {
        if ( seed != null ) {
            distribution.reseedRandomGenerator( seed );
        }
        double[][] matrix = new double[rows][cols];
        double[] samples = distribution.sample( rows * cols );
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                matrix[i][j] = samples[i + rows * j];
            }
        }
        return matrix;
    }

    private static double[][] randomExpressionMatrix( int rows, int cols, IntegerDistribution distribution ) {
        if ( seed != null ) {
            distribution.reseedRandomGenerator( seed );
        }
        double[][] matrix = new double[rows][cols];
        int[] samples = distribution.sample( rows * cols );
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                matrix[i][j] = samples[i + rows * j];
            }
        }
        return matrix;
    }

    private static class NegativeBinomialDistribution implements IntegerDistribution {

        private final int i;
        private final double v;
        private cern.jet.random.NegativeBinomial distribution;

        private NegativeBinomialDistribution( int i, double v ) {
            this.i = i;
            this.v = v;
            this.distribution = new cern.jet.random.NegativeBinomial( i, v, new MersenneTwister() );
        }

        @Override
        public double probability( int x ) {
            return distribution.pdf( x );
        }

        @Override
        public double cumulativeProbability( int x ) {
            return distribution.cdf( x );
        }

        @Override
        public double cumulativeProbability( int x0, int x1 ) throws NumberIsTooLargeException {
            return cumulativeProbability( x1 ) - cumulativeProbability( x0 );
        }

        @Override
        public int inverseCumulativeProbability( double p ) throws OutOfRangeException {
            return 0;
        }

        @Override
        public double getNumericalMean() {
            return i * ( 1 - v ) / v;
        }

        @Override
        public double getNumericalVariance() {
            return i * ( 1 - v ) / ( v * v );
        }

        @Override
        public int getSupportLowerBound() {
            return 0;
        }

        @Override
        public int getSupportUpperBound() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isSupportConnected() {
            return true;
        }

        @Override
        public void reseedRandomGenerator( long seed ) {
            this.distribution = new cern.jet.random.NegativeBinomial( i, v, new MersenneTwister( ( int ) seed ) );
        }

        @Override
        public int sample() {
            return distribution.nextInt();
        }

        @Override
        public int[] sample( int sampleSize ) {
            int[] X = new int[sampleSize];
            for ( int i = 0; i < X.length; i++ ) {
                X[i] = sample();
            }
            return X;
        }
    }
}
