package ubic.gemma.persistence.service.expression.experiment;

import cern.jet.random.NegativeBinomial;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.NegativeBinomialDistribution;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ubic.gemma.persistence.util.ByteArrayUtils.doubleArrayToBytes;

public class SingleCellTestUtils {

    private static final NegativeBinomialDistribution countDistribution = new NegativeBinomialDistribution( 6, 0.5 );
    private static final UniformRealDistribution uniform100Distribution = new UniformRealDistribution( 0, 100 );
    private static final Random random = new Random();

    /**
     * Set the seed used to generate random single-cell vectors.
     */
    public static void setSeed( int seed ) {
        countDistribution.reseedRandomGenerator( seed );
        uniform100Distribution.reseedRandomGenerator( seed );
        random.setSeed( seed );
    }

    public static Collection<SingleCellExpressionDataVector> randomSingleCellVectors() {
        return randomSingleCellVectors( 100, 4, 1000, 0.9 );
    }

    public static Collection<SingleCellExpressionDataVector> randomSingleCellVectors( int numDesignElements, int numSamples, int numCellsPerBioAssay, double sparsity ) {
        ArrayDesign arrayDesign = new ArrayDesign();
        for ( int i = 0; i < numDesignElements; i++ ) {
            arrayDesign.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < numSamples; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, arrayDesign, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return randomSingleCellVectors( ee, arrayDesign, qt, numCellsPerBioAssay, sparsity );
    }

    /**
     * Generate random single-cell vectors with 1000 cells/sample and 10% sparsity.
     * @see #randomSingleCellVectors(ExpressionExperiment, ArrayDesign, QuantitationType, int, double)
     */
    public static Collection<SingleCellExpressionDataVector> randomSingleCellVectors( ExpressionExperiment ee, ArrayDesign ad, QuantitationType qt ) {
        return randomSingleCellVectors( ee, ad, qt, 1000, 0.9 );
    }

    /**
     * Generate random single-cell vectors.
     * <p>
     * Counts are drawn from a {@link NegativeBinomial}.
     * <p>
     * One vector is generated by design element from the provided array design with the given sparsity.
     * @param numCellsPerBioAssay how many cells to generate per {@link BioAssay}
     * @param sparsity            sparsity of the vectors
     */
    public static Collection<SingleCellExpressionDataVector> randomSingleCellVectors( ExpressionExperiment ee, ArrayDesign ad, QuantitationType qt, int numCellsPerBioAssay, double sparsity ) {
        Assert.isTrue( qt.getGeneralType() == GeneralType.QUANTITATIVE );
        Assert.isTrue( qt.getType() == StandardQuantitationType.COUNT );
        Assert.isTrue( qt.getScale() == ScaleType.COUNT || qt.getScale() == ScaleType.LOG2 || qt.getScale() == ScaleType.LOG1P || qt.getScale() == ScaleType.PERCENT );
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.DOUBLE,
                "Can only generate double vectors." );
        Assert.isTrue( sparsity >= 0.0 && sparsity <= 1.0, "Sparsity must be between 0 and 1." );
        List<BioAssay> samples = ee.getBioAssays().stream()
                // for reproducibility
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .collect( Collectors.toList() );
        int numCells = numCellsPerBioAssay * ee.getBioAssays().size();
        SingleCellDimension dimension = new SingleCellDimension();
        dimension.setName( "Bunch of test cells" );
        dimension.setCellIds( IntStream.rangeClosed( 1, numCells ).mapToObj( Integer::toString ).collect( Collectors.toList() ) );
        dimension.setNumberOfCells( numCells );
        dimension.setBioAssays( samples );
        int[] offsets = new int[samples.size()];
        for ( int i = 0; i < samples.size(); i++ ) {
            offsets[i] = i * numCellsPerBioAssay;
        }
        dimension.setBioAssaysOffset( offsets );
        // necessary for reproducible results
        List<CompositeSequence> sortedCs = new ArrayList<>( ad.getCompositeSequences() );
        sortedCs.sort( Comparator.comparing( CompositeSequence::getName ) );
        List<SingleCellExpressionDataVector> results = new ArrayList<>();
        for ( CompositeSequence compositeSequence : sortedCs ) {
            SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
            vector.setExpressionExperiment( ee );
            vector.setDesignElement( compositeSequence );
            vector.setQuantitationType( qt );
            vector.setSingleCellDimension( dimension );
            double density = 1.0 - sparsity;
            int N = ( int ) Math.ceil( density * numCells );
            double[] X = new double[N];
            int[] IX = new int[X.length];
            int step = ( int ) ( 1.0 / density );
            for ( int i = 0; i < N; i++ ) {
                if ( qt.getScale() == ScaleType.PERCENT ) {
                    X[i] = uniform100Distribution.sample();
                } else {
                    X[i] = transform( countDistribution.sample(), qt.getScale() );
                }
                IX[i] = ( i * step ) + random.nextInt( step );
            }
            vector.setData( doubleArrayToBytes( X ) );
            vector.setDataIndices( IX );
            results.add( vector );
        }
        return results;
    }

    private static double transform( int i, ScaleType scale ) {
        switch ( scale ) {
            case LINEAR:
            case COUNT:
                return i;
            case LOG2:
                return Math.log( i ) / Math.log( 2 );
            case LOG1P:
                return Math.log1p( i );
            default:
                throw new IllegalArgumentException( "Unsupported scale type: " + scale );
        }
    }
}