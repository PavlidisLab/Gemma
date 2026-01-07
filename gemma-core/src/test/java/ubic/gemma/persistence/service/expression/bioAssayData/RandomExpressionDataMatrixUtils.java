package ubic.gemma.persistence.service.expression.bioAssayData;

import cern.colt.matrix.DoubleMatrix1D;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.math3.distribution.*;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationScaleConversionException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.convertData;

/**
 * Utilities for generating random {@link ExpressionDataDoubleMatrix} following various random distributions.
 *
 * @see RandomSingleCellDataUtils
 * @see RandomBulkDataUtils
 */
public class RandomExpressionDataMatrixUtils {

    /**
     * Seed used for generating random matrices.
     */
    private static Long seed;

    public static void setSeed( long seed ) {
        RandomExpressionDataMatrixUtils.seed = seed;
    }

    /**
     * Generate a count matrix.
     * <p>
     * The counts are drawn from a Negative Binomial distribution.
     */
    public static ExpressionDataDoubleMatrix randomCountMatrix( ExpressionExperiment ee ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomExpressionMatrix( ee, qt, new NegativeBinomialDistribution( 6, 0.5 ) );
    }

    /**
     * Generate a "transformed" count matrix.
     */
    public static ExpressionDataDoubleMatrix randomCountMatrix( ExpressionExperiment ee, ScaleType scaleType ) {
        ExpressionDataDoubleMatrix matrix = randomCountMatrix( ee );
        for ( QuantitationType qt : matrix.getQuantitationTypes() ) {
            qt.setScale( scaleType );
        }
        if ( scaleType == ScaleType.COUNT || scaleType == ScaleType.LINEAR ) {
            return matrix;
        }
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                double val = matrix.getAsDouble( i, j );
                try {
                    matrix.set( i, j, convertData( new double[] { val }, StandardQuantitationType.COUNT, ScaleType.COUNT, scaleType )[0] );
                } catch ( UnsupportedQuantitationScaleConversionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
        return matrix;
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

    /**
     * Create a random matrix with a specific sample structure.
     */
    public static ExpressionDataDoubleMatrix randomLog2Matrix( ExpressionExperiment ee, BioAssayDimension dimension ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Set<ArrayDesign> ads = new HashSet<>();
        List<BioMaterial> samples = new ArrayList<>( dimension.getBioAssays().size() );
        for ( BioAssay assay : dimension.getBioAssays() ) {
            if ( !ee.getBioAssays().contains( assay ) && !CollectionUtils.containsAny( assay.getSampleUsed().getAllBioAssaysUsedIn(), ee.getBioAssays() ) ) {
                throw new IllegalStateException( assay + " does not belong to " + ee + "." );
            }
            samples.add( assay.getSampleUsed() );
            ads.add( assay.getArrayDesignUsed() );
        }
        if ( ads.size() != 1 ) {
            throw new IllegalStateException( "Assays must use exactly one platform." );
        }
        List<CompositeSequence> designElements = ads.iterator().next().getCompositeSequences().stream().sorted( Comparator.comparing( CompositeSequence::getName ) ).collect( Collectors.toList() );
        return randomExpressionMatrix( ee, qt, designElements, samples, new TruncatedNormalDistribution( 6.25, 1.46, 0, Double.POSITIVE_INFINITY ) );
    }

    public static ExpressionDataDoubleMatrix randomExpressionMatrix( ExpressionExperiment ee, QuantitationType qt, RealDistribution distribution ) {
        List<BioMaterial> samples = ee.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .sorted( Comparator.comparing( BioMaterial::getName ) )
                .collect( Collectors.toList() );
        Set<ArrayDesign> ads = ee.getBioAssays().stream().map( BioAssay::getArrayDesignUsed ).collect( Collectors.toSet() );
        if ( ads.size() != 1 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must use exactly one platform." );
        }
        List<CompositeSequence> designElements = ads.iterator().next().getCompositeSequences().stream()
                .sorted( Comparator.comparing( CompositeSequence::getName ) )
                .collect( Collectors.toList() );
        return randomExpressionMatrix( ee, qt, designElements, samples, distribution );
    }

    public static ExpressionDataDoubleMatrix randomExpressionMatrix( ExpressionExperiment ee, QuantitationType qt, List<CompositeSequence> designElements, List<BioMaterial> samples, RealDistribution distribution ) {
        int numSamples = samples.size();
        if ( numSamples == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one sample." );
        }
        int numVectors = designElements.size();
        if ( numVectors == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one design element." );
        }
        DenseDoubleMatrix<CompositeSequence, BioMaterial> matrix = new DenseDoubleMatrix<>( randomExpressionMatrix( numVectors, numSamples, distribution ) );
        matrix.setRowNames( designElements );
        matrix.setColumnNames( samples );
        return new ExpressionDataDoubleMatrix( ee, matrix, qt );
    }

    private static ExpressionDataDoubleMatrix randomExpressionMatrix( ExpressionExperiment ee, QuantitationType qt, IntegerDistribution distribution ) {
        List<BioMaterial> samples = ee.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .sorted( Comparator.comparing( BioMaterial::getName ) )
                .collect( Collectors.toList() );
        return randomExpressionMatrix( ee, qt, samples, distribution );
    }

    private static ExpressionDataDoubleMatrix randomExpressionMatrix( ExpressionExperiment ee, QuantitationType qt, List<BioMaterial> samples, IntegerDistribution distribution ) {
        int numSamples = samples.size();
        if ( numSamples == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one bioassay." );
        }
        int numVectors = ee.getBioAssays().iterator().next().getArrayDesignUsed().getCompositeSequences().size();
        if ( numVectors == 0 ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have at least one probe." );
        }
        DenseDoubleMatrix<CompositeSequence, BioMaterial> matrix = new DenseDoubleMatrix<>( randomExpressionMatrix( numVectors, numSamples, distribution ) );
        matrix.setRowNames( new ArrayList<>( ee.getBioAssays().iterator().next().getArrayDesignUsed().getCompositeSequences() ) );
        matrix.setColumnNames( samples );
        return new ExpressionDataDoubleMatrix( ee, matrix, qt );
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

    public static ExpressionDataDoubleMatrix randomLog2cpmMatrix( ExpressionExperiment ee ) {
        QuantitationType log2cpmQt = QuantitationType.Factory.newInstance();
        log2cpmQt.setName( "log2cpm" );
        log2cpmQt.setGeneralType( GeneralType.QUANTITATIVE );
        log2cpmQt.setType( StandardQuantitationType.AMOUNT );
        log2cpmQt.setScale( ScaleType.LOG2 );
        log2cpmQt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix dm = randomCountMatrix( ee );
        DoubleMatrix1D librarySize = MatrixStats.colSums( dm.getMatrix() );
        DoubleMatrix<CompositeSequence, BioMaterial> log2cpmData = MatrixStats.convertToLog2Cpm( dm.getMatrix(), librarySize );
        return dm.withMatrix( log2cpmData, Collections.singletonMap( dm.getQuantitationType(), log2cpmQt ) );
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
