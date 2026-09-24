package ubic.gemma.core.analysis.preprocess.filter;

import cern.colt.matrix.DoubleMatrix1D;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.randomBulkVectors;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.setSeed;

public class RepetitiveValuesFilterTest {

    @Test
    public void testFilterLog2cpmData() throws FilteringException {
        setSeed( 123 );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( ee, randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        // fill a row with zeroes
        CompositeSequence deToDrop = countMatrix.getDesignElementForRow( 5 );
        DoubleMatrix<CompositeSequence, BioMaterial> cm = countMatrix.getMatrix().copy();
        for ( int j = 0; j < cm.columns(); j++ ) {
            cm.set( 5, j, 0.0 );
        }

        countMatrix = countMatrix.withMatrix( cm );

        // filtering the count matrix should drop the row with all zeroes easily
        assertThat( new RepetitiveValuesFilter().filter( countMatrix ) )
                .satisfies( fm -> {
                    assertThat( fm.rows() ).isEqualTo( 99 );
                    assertThat( fm.getDesignElements() )
                            .doesNotContain( deToDrop );
                } );

        // calculate library sizes
        DoubleMatrix1D librarySize = MatrixStats.colSums( new DenseDoubleMatrix<>( countMatrix.getRawMatrixAsDoubles() ) );
        for ( int i = 0; i < librarySize.size(); i++ ) {
            countMatrix.getBioAssayDimension().getBioAssays().get( i )
                    .setSequenceReadCount( Math.round( librarySize.get( i ) ) );
        }
        // normalize
        QuantitationType log2cpmQt = new QuantitationType();
        log2cpmQt.setName( "log2cpm" );
        log2cpmQt.setGeneralType( GeneralType.QUANTITATIVE );
        log2cpmQt.setType( StandardQuantitationType.AMOUNT );
        log2cpmQt.setScale( ScaleType.LOG2 );
        log2cpmQt.setRepresentation( PrimitiveType.DOUBLE );
        DoubleMatrix<CompositeSequence, BioMaterial> log2cpmM = countMatrix.getMatrix().copy();

        for ( int i = 0; i < log2cpmM.rows(); i++ ) {
            for ( int j = 0; j < log2cpmM.columns(); j++ ) {
                log2cpmM.set( i, j, Math.log( 1e6 * ( log2cpmM.get( i, j ) + 0.5 ) / ( librarySize.get( j ) + 1.0 ) ) / Math.log( 2 ) );
            }
        }

        ExpressionDataDoubleMatrix log2cpmMatrix = new ExpressionDataDoubleMatrix( ee, log2cpmM, log2cpmQt );
        // calculate log2cpm
        ExpressionDataDoubleMatrix filteredMatrix = new RepetitiveValuesFilter().filter( log2cpmMatrix );
        assertThat( filteredMatrix.rows() ).isEqualTo( 99 );
        assertThat( filteredMatrix.getDesignElements() )
                .doesNotContain( deToDrop );
    }

    @Test
    public void testQuantileNormalizedData() throws FilteringException {
        setSeed( 123 );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( ee, randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        // fill a row with zeroes
        CompositeSequence deToDrop = countMatrix.getDesignElementForRow( 5 );
        DoubleMatrix<CompositeSequence, BioMaterial> cm = countMatrix.getMatrix().copy();
        for ( int j = 0; j < cm.columns(); j++ ) {
            cm.set( 5, j, 0.0 );
        }

        countMatrix = countMatrix.withMatrix( cm );

        // filtering the count matrix should drop the row with all zeroes easily
        assertThat( new RepetitiveValuesFilter().filter( countMatrix ) )
                .satisfies( fm -> {
                    assertThat( fm.rows() ).isEqualTo( 99 );
                    assertThat( fm.getDesignElements() )
                            .doesNotContain( deToDrop );
                } );

        // calculate library sizes
        DoubleMatrix1D librarySize = MatrixStats.colSums( new DenseDoubleMatrix<>( countMatrix.getRawMatrixAsDoubles() ) );
        for ( int i = 0; i < librarySize.size(); i++ ) {
            countMatrix.getBioAssayDimension().getBioAssays().get( i )
                    .setSequenceReadCount( Math.round( librarySize.get( i ) ) );
        }
        // normalize
        QuantitationType log2cpmQt = new QuantitationType();
        log2cpmQt.setName( "log2cpm" );
        log2cpmQt.setGeneralType( GeneralType.QUANTITATIVE );
        log2cpmQt.setType( StandardQuantitationType.AMOUNT );
        log2cpmQt.setScale( ScaleType.LOG2 );
        log2cpmQt.setRepresentation( PrimitiveType.DOUBLE );
        DoubleMatrix<CompositeSequence, BioMaterial> log2cpmM = countMatrix.getMatrix().copy();

        for ( int i = 0; i < log2cpmM.rows(); i++ ) {
            for ( int j = 0; j < log2cpmM.columns(); j++ ) {
                log2cpmM.set( i, j, Math.log( 1e6 * ( log2cpmM.get( i, j ) + 0.5 ) / ( librarySize.get( j ) + 1.0 ) ) / Math.log( 2 ) );
            }
        }

        ExpressionDataDoubleMatrix log2cpmMatrix = new ExpressionDataDoubleMatrix( ee, log2cpmM, log2cpmQt );

        DoubleMatrix<CompositeSequence, BioMaterial> normalizedLog2cpm = new QuantileNormalizer()
                .normalize( log2cpmMatrix.getMatrix() );

        QuantitationType normalizedQt = QuantitationType.Factory.newInstance( log2cpmQt );
        normalizedQt.setIsNormalized( true );

        ExpressionDataDoubleMatrix normalizedLg2fcMatrix = new ExpressionDataDoubleMatrix( ee, normalizedLog2cpm, normalizedQt );

        ExpressionDataDoubleMatrix filteredMatrix = new RepetitiveValuesFilter().filter( normalizedLg2fcMatrix );
        assertThat( filteredMatrix.rows() ).isEqualTo( 99 );
        assertThat( filteredMatrix.getDesignElements() )
                .doesNotContain( deToDrop );
    }

    /**
     * A probe that is the highest-expressed in every sample holds one single rank throughout, because
     * ranking runs within a column across design elements. It has
     * a distinct value in every sample and can be differentially expressed, so the rank-based mode must keep it; only
     * rows whose own values repeat are removable.
     */
    @Test
    public void testTopRankedProbeWithDistinctValuesIsKept() throws FilteringException {
        int numProbes = 9, numSamples = 22;
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < numProbes; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < numSamples; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );

        DoubleMatrix<CompositeSequence, BioMaterial> m = new DenseDoubleMatrix<>( numProbes, numSamples );
        m.setRowNames( new ArrayList<>( ad.getCompositeSequences() ) );
        m.setColumnNames( ee.getBioAssays().stream().map( BioAssay::getSampleUsed ).collect( Collectors.toList() ) );
        for ( int i = 0; i < numProbes; i++ ) {
            for ( int j = 0; j < numSamples; j++ ) {
                // ordinary probes: values repeat, and the rank order genuinely moves between samples, so the rank
                // measure keeps them on its own and they do not lean on the all-distinct guard under test
                m.set( i, j, 10.0 + ( ( i * 3 + j ) % 7 ) );
            }
        }
        // the probe that is top-expressed in EVERY sample, with a distinct value each time
        int top = 3;
        CompositeSequence topRanked = m.getRowName( top );
        for ( int j = 0; j < numSamples; j++ ) {
            m.set( top, j, 100.0 + j );
        }
        // the near-constant probe: one value everywhere but the last sample, as in GSE8441's "constant" row
        int flat = 6;
        CompositeSequence nearConstant = m.getRowName( flat );
        for ( int j = 0; j < numSamples; j++ ) {
            m.set( flat, j, j == numSamples - 1 ? 2.301 : 2.3 );
        }

        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, m, qt );

        RepetitiveValuesFilter filter = new RepetitiveValuesFilter();
        filter.setMode( RepetitiveValuesFilter.Mode.RANK );
        filter.setMinimumFractionOfUniqueValues( 0.1 );

        assertThat( filter.filter( matrix ) )
                .satisfies( fm -> {
                    assertThat( fm.getDesignElements() ).contains( topRanked );
                    assertThat( fm.getDesignElements() ).doesNotContain( nearConstant );
                    assertThat( fm.rows() ).isEqualTo( numProbes - 1 );
                } );
    }

    @Test
    public void testRepetitiveValuesWithNonZeroVariance() throws FilteringException {
        setSeed( 123 );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( ee, randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        // only fill half the row with zeroes
        CompositeSequence deToDrop = countMatrix.getDesignElementForRow( 5 );
        DoubleMatrix<CompositeSequence, BioMaterial> cm = countMatrix.getMatrix().copy();
        for ( int j = 0; j < 10; j++ ) {
            if ( j < 7 ) {
                cm.set( 5, j, 0.0 );
            } else {
                cm.set( 5, j, ( double ) j - 8.0 );
            }
        }

        countMatrix = countMatrix.withMatrix( cm );

        // filtering the count matrix should drop the row with all zeroes easily
        assertThat( new RepetitiveValuesFilter().filter( countMatrix ) )
                .satisfies( fm -> {
                    assertThat( fm.rows() ).isEqualTo( 99 );
                    assertThat( fm.getDesignElements() )
                            .doesNotContain( deToDrop );
                } );
    }

    @Test
    public void testRepetitiveValuesWithCountData() throws FilteringException {
        setSeed( 123 );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( ee, randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        // only fill half the row with zeroes
        CompositeSequence deToDrop = countMatrix.getDesignElementForRow( 5 );
        DoubleMatrix<CompositeSequence, BioMaterial> cm = countMatrix.getMatrix().copy();
        for ( int j = 0; j < 10; j++ ) {
            if ( j < 8 ) {
                cm.set( 5, j, 0.0 );
            } else {
                cm.set( 5, j, ( double ) j - 8.0 );
            }
        }

        countMatrix = countMatrix.withMatrix( cm );

        // filtering the count matrix should drop the row with all zeroes easily
        assertThat( new RepetitiveValuesFilter().filter( countMatrix ) )
                .satisfies( fm -> {
                    assertThat( fm.rows() ).isEqualTo( 99 );
                    assertThat( fm.getDesignElements() )
                            .doesNotContain( deToDrop );
                } );
    }

    @Test
    public void testFilterWithNonLogScaleData() throws FilteringException {
        setSeed( 123 );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( ee, randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        // only fill half the row with zeroes
        CompositeSequence deToDrop = countMatrix.getDesignElementForRow( 5 );
        DoubleMatrix<CompositeSequence, BioMaterial> cm = countMatrix.getMatrix().copy();
        for ( int j = 0; j < 10; j++ ) {
            if ( j < 8 ) {
                cm.set( 5, j, 0.0 );
            } else {
                cm.set( 5, j, ( double ) j - 8.0 );
            }
        }

        countMatrix = countMatrix.withMatrix( cm );

        // filtering the count matrix should drop the row with all zeroes easily
        assertThat( new RepetitiveValuesFilter().filter( countMatrix ) )
                .satisfies( fm -> {
                    assertThat( fm.rows() ).isEqualTo( 99 );
                    assertThat( fm.getDesignElements() )
                            .doesNotContain( deToDrop );
                } );
    }

}
