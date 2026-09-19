package ubic.gemma.core.loader.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link DataUpdaterImpl} with every collaborator mocked. The orchestrators run without a transaction, so each step
 * commits on its own and a failure part way through leaves the earlier steps in place.
 */
public class DataUpdaterImplTest {

    private DataUpdaterImpl dataUpdater;

    private DataUpdater self;
    private BioAssayService bioAssayService;
    private ExpressionExperimentService experimentService;
    private PreprocessorService preprocessorService;
    private QuantitationTypeService qtService;
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    private ArrayDesign platform;
    private List<CompositeSequence> elements;
    private ExpressionExperiment ee;
    private List<BioAssay> assays;

    @BeforeEach
    public void setUp() {
        dataUpdater = new DataUpdaterImpl();
        self = mock();
        ArrayDesignService arrayDesignService = mock();
        BioAssayDimensionService bioAssayDimensionService = mock();
        bioAssayService = mock();
        experimentService = mock();
        preprocessorService = mock();
        qtService = mock();
        rawExpressionDataVectorService = mock();
        ReflectionTestUtils.setField( dataUpdater, "self", self );
        ReflectionTestUtils.setField( dataUpdater, "arrayDesignService", arrayDesignService );
        ReflectionTestUtils.setField( dataUpdater, "assayDimensionService", bioAssayDimensionService );
        ReflectionTestUtils.setField( dataUpdater, "bioAssayDimensionService", bioAssayDimensionService );
        ReflectionTestUtils.setField( dataUpdater, "bioAssayService", bioAssayService );
        ReflectionTestUtils.setField( dataUpdater, "experimentService", experimentService );
        ReflectionTestUtils.setField( dataUpdater, "preprocessorService", preprocessorService );
        ReflectionTestUtils.setField( dataUpdater, "qtService", qtService );
        ReflectionTestUtils.setField( dataUpdater, "rawExpressionDataVectorService", rawExpressionDataVectorService );
        ReflectionTestUtils.setField( dataUpdater, "rawAndProcessedExpressionDataVectorService", mock( RawAndProcessedExpressionDataVectorService.class ) );
        ReflectionTestUtils.setField( dataUpdater, "dataUpdaterAuditService", mock( DataUpdaterAuditService.class ) );

        platform = ArrayDesign.Factory.newInstance();
        platform.setId( 1L );
        platform.setShortName( "GPL1" );
        elements = new ArrayList<>();
        for ( long i = 1; i <= 2; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "gene" + i, platform );
            cs.setId( i );
            platform.getCompositeSequences().add( cs );
            elements.add( cs );
        }

        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        assays = new ArrayList<>();
        for ( long i = 1; i <= 3; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "sample" + i );
            bm.setId( i );
            BioAssay ba = BioAssay.Factory.newInstance( "assay" + i, platform, bm );
            ba.setId( i );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            assays.add( ba );
        }

        when( experimentService.thaw( ee ) ).thenReturn( ee );
        when( experimentService.thawLite( ee ) ).thenReturn( ee );
        when( arrayDesignService.thaw( platform ) ).thenReturn( platform );
        when( experimentService.getArrayDesignsUsed( ee ) ).thenReturn( Collections.singleton( platform ) );
        when( bioAssayDimensionService.findOrCreate( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
    }

    @Test
    public void testReplaceDataReportsAPostprocessingFailureAfterTheRawDataWasReplaced() {
        doThrow( new PreprocessingException( ee, "PCA failed" ) ).when( preprocessorService ).process( ee );

        assertThatThrownBy( () -> dataUpdater.replaceData( ee, platform, doubleQt( "test data" ), matrix( "gene1", "gene2" ) ) )
                .isInstanceOf( RawDataPostprocessingException.class )
                .hasMessageContaining( "GSE1" )
                .hasMessageContaining( "The raw data was replaced" )
                .hasMessageContaining( "must be regenerated" )
                .hasRootCauseMessage( "Failed to pre-process GSE1: PCA failed" );
        verify( experimentService ).replaceAllRawDataVectors( same( ee ), anyCollection() );
    }

    @Test
    public void testAddCountDataStillStoresTheCountsWhenPostprocessingTheLog2cpmFails() {
        doThrow( new PreprocessingException( ee, "PCA failed" ) ).when( preprocessorService ).process( ee );

        assertThatThrownBy( () -> dataUpdater.addCountData( ee, platform, matrix( "gene1", "gene2" ), null,
                Collections.emptyMap(), false ) )
                .isInstanceOf( RawDataPostprocessingException.class )
                .hasMessageContaining( "The raw data was replaced" );
        // log2cpm replaced the raw data (and removed the old counts) ...
        verify( experimentService ).replaceAllRawDataVectors( same( ee ), anyCollection() );
        // ... so the counts are stored again, and the library sizes recorded, before the failure is reported
        verify( self ).addData( same( ee ), same( platform ), argThat( ( ExpressionDataDoubleMatrix m ) ->
                m.getQuantitationTypes().iterator().next().getType() == StandardQuantitationType.COUNT ) );
        verify( bioAssayService, times( 3 ) ).update( any( BioAssay.class ) );
    }

    @Test
    public void testLog2cpmFromCountsReportsAFailureAndRestoresTheCountsAsPreferred() {
        QuantitationType countQt = countQt();
        when( rawExpressionDataVectorService.find( countQt ) ).thenReturn( countVectors( countQt ) );
        doThrow( new RuntimeException( "database unavailable" ) )
                .doNothing()
                .when( qtService ).update( countQt );

        assertThatThrownBy( () -> dataUpdater.log2cpmFromCounts( ee, countQt ) )
                .hasMessageContaining( "Failed to compute log2cpm" )
                .hasMessageContaining( "GSE1" )
                .hasRootCauseMessage( "database unavailable" );
        assertThat( countQt.getIsPreferred() ).isTrue();
        verify( qtService, times( 2 ) ).update( countQt );
        verifyNoInteractions( self );
    }

    @Test
    public void testLog2cpmFromCountsLeavesTheCountsNonPreferredWhenOnlyPostprocessingFails() {
        QuantitationType countQt = countQt();
        when( rawExpressionDataVectorService.find( countQt ) ).thenReturn( countVectors( countQt ) );
        RawDataPostprocessingException failure = new RawDataPostprocessingException( ee, "added", new RuntimeException( "PCA failed" ) );
        doThrow( failure ).when( self ).addData( same( ee ), same( platform ), any() );

        assertThatThrownBy( () -> dataUpdater.log2cpmFromCounts( ee, countQt ) )
                .isSameAs( failure );
        // the log2cpm data was stored as the preferred data, so the counts must not be made preferred again
        assertThat( countQt.getIsPreferred() ).isFalse();
        verify( qtService, times( 1 ) ).update( countQt );
    }

    /**
     * With {@code -allowMissing}, the samples missing from the count matrix are removed and committed. A matrix whose
     * rows match no platform element must be rejected before that happens.
     */
    @Test
    public void testAddCountDataRemovesNoSampleWhenNoRowMatchesThePlatform() {
        DoubleMatrix<String, String> counts = matrix( Arrays.asList( "sample1", "sample2" ), "notOnPlatform1", "notOnPlatform2" );

        assertThatThrownBy( () -> dataUpdater.addCountData( ee, platform, counts, null, Collections.emptyMap(), true ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "None of the rows matched the given platform elements" );
        assertThat( ee.getBioAssays() ).hasSize( 3 );
        verify( experimentService, never() ).update( any( ExpressionExperiment.class ) );
        verify( bioAssayService, never() ).remove( any( BioAssay.class ) );
    }

    private DoubleMatrix<String, String> matrix( String... rowNames ) {
        return matrix( Arrays.asList( "sample1", "sample2", "sample3" ), rowNames );
    }

    private DoubleMatrix<String, String> matrix( List<String> colNames, String... rowNames ) {
        DenseDoubleMatrix<String, String> m = new DenseDoubleMatrix<>( rowNames.length, colNames.size() );
        for ( int i = 0; i < rowNames.length; i++ ) {
            for ( int j = 0; j < colNames.size(); j++ ) {
                m.set( i, j, 10.0 + i + j );
            }
        }
        m.setRowNames( Arrays.asList( rowNames ) );
        m.setColumnNames( colNames );
        return m;
    }

    private QuantitationType doubleQt( String name ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( name );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return qt;
    }

    private QuantitationType countQt() {
        QuantitationType qt = doubleQt( "Counts" );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setIsPreferred( true );
        return qt;
    }

    private Collection<RawExpressionDataVector> countVectors( QuantitationType qt ) {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance( assays );
        List<RawExpressionDataVector> vectors = new ArrayList<>();
        for ( CompositeSequence cs : elements ) {
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qt );
            v.setDesignElement( cs );
            v.setBioAssayDimension( bad );
            v.setDataAsDoubles( new double[] { 10, 20, 30 } );
            vectors.add( v );
        }
        return vectors;
    }
}
