package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataRemovedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.only;

/**
 * Tests covering integration of single-cell.
 */
@ContextConfiguration
public class SingleCellExpressionExperimentServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class SingleCellExpressionExperimentServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return new SingleCellExpressionExperimentServiceImpl();
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public ExperimentalFactorService experimentalFactorService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }
    }

    @Autowired
    private SingleCellExpressionExperimentService scExpressionExperimentService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    private ArrayDesign ad;
    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        for ( int i = 0; i < 10; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "test" + i );
            cs.setArrayDesign( ad );
            ad.getCompositeSequences().add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.setTaxon( taxon );
        // TODO: model bioassays against sub-biomaterial to represent cell aggregates
        BioMaterial bm = BioMaterial.Factory.newInstance( "a", taxon );
        sessionFactory.getCurrentSession().persist( bm );
        ee.getBioAssays().add( BioAssay.Factory.newInstance( "a", ad, bm ) );
        ee.getBioAssays().add( BioAssay.Factory.newInstance( "b", ad, bm ) );
        ee.getBioAssays().add( BioAssay.Factory.newInstance( "c", ad, bm ) );
        ee.getBioAssays().add( BioAssay.Factory.newInstance( "d", ad, bm ) );
        ee = expressionExperimentDao.create( ee );
    }

    @After
    public void resetMocks() {
        reset( auditTrailService );
    }

    @Test
    public void testGetSingleCellDataMatrix() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        SingleCellExpressionDataMatrix<Double> matrix = scExpressionExperimentService.getSingleCellExpressionDataMatrix( ee, qt );
        assertThat( matrix.getQuantitationType() ).isEqualTo( qt );
        assertThat( matrix.getSingleCellDimension() ).isEqualTo( scd );
        assertThat( matrix.columns() ).isEqualTo( 100 );
        assertThat( matrix.rows() ).isEqualTo( 100 );
    }

    @Test
    public void testAddSingleCellDataVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );

        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .contains( vectors.iterator().next().getQuantitationType() );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .allSatisfy( v -> assertThat( v.getId() ).isNotNull() );

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .hasSize( 1 )
                .allSatisfy( scd -> {
                    assertThat( scd.getCellTypeAssignments().iterator().next().getCellType( 0 ).getValue() ).isEqualTo( "A" );
                    assertThat( scd.getCellTypeAssignments().iterator().next().getCellType( 50 ).getValue() ).isEqualTo( "B" );
                } );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors2.iterator().next().getQuantitationType(), vectors2 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .hasSize( 2 );

        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
    }

    @Test
    public void testAddSingleCellDataVectorsTwice() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        sessionFactory.getCurrentSession().flush();
        assertThatThrownBy( () -> scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "already have vectors for the quantitation type" );
        verify( auditTrailService, only() ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
    }

    @Test
    public void testAddSingleCellDataVectorsWhenThereIsAlreadyAPreferredSetOfVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        sessionFactory.getCurrentSession().flush();
        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors2.iterator().next().getQuantitationType(), vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 )
                .satisfiesOnlyOnce( qt -> assertThat( qt.getIsPreferred() ).isTrue() );
        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
    }

    @Test
    public void testReplaceVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( qt );
        scExpressionExperimentService.replaceSingleCellDataVectors( ee, qt, vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .doesNotContainAnyElementsOf( vectors )
                .containsAll( vectors2 );

        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataReplacedEvent.class ), any() );
    }

    @Test
    public void testRemoveVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( false );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        scExpressionExperimentService.removeSingleCellDataVectors( ee, qt );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataRemovedEvent.class ), any() );
    }

    @Test
    public void testRemoveVectorsSharingADimension() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( vectors.iterator().next().getSingleCellDimension() );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( qt2 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        scExpressionExperimentService.removeSingleCellDataVectors( ee, qt );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).doesNotContain( qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataRemovedEvent.class ), any() );
    }

    @Test
    public void testRelabelCellTypes() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( scExpressionExperimentService.getCellTypeLabellings( ee ) )
                .hasSize( 1 );
        assertThat( scExpressionExperimentService.getPreferredCellTypeLabelling( ee ) ).isNotNull();
        assertThat( scExpressionExperimentService.getCellTypes( ee ) ).hasSize( 2 )
                .extracting( Characteristic::getValue )
                .containsExactlyInAnyOrder( "A", "B" );
        String[] ct = new String[100];
        for ( int i = 0; i < ct.length; i++ ) {
            ct[i] = i < 75 ? "A" : "B";
        }
        CellTypeAssignment newLabelling = scExpressionExperimentService.relabelCellTypes( ee, scd, Arrays.asList( ct ), null, null );
        assertThat( newLabelling ).satisfies( l -> {
            assertThat( l.getId() ).isNotNull();
            assertThat( l.isPreferred() ).isTrue();
        } );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .allSatisfy( v -> assertThat( v.getSingleCellDimension().getCellTypeAssignments() ).contains( newLabelling ) );
        assertThat( scExpressionExperimentService.getCellTypeLabellings( ee ) )
                .hasSize( 1 )
                .contains( newLabelling );
        assertThat( scExpressionExperimentService.getPreferredCellTypeLabelling( ee ) ).isEqualTo( newLabelling );
        assertThat( scExpressionExperimentService.getCellTypes( ee ) ).hasSize( 2 )
                .extracting( Characteristic::getValue )
                .containsExactlyInAnyOrder( "A", "B" );

        scExpressionExperimentService.removeCellTypeLabels( ee, scd, newLabelling );
        assertThat( scExpressionExperimentService.getPreferredCellTypeLabelling( ee ) ).isNull();

        // FIXME: add proper assertions for the created factor, but the ExperimentalFactorService is mocked
        verify( experimentalFactorService, times( 2 ) ).create( any( ExperimentalFactor.class ) );
        verify( experimentalFactorService, times( 2 ) ).remove( any( ExperimentalFactor.class ) );
    }

    /**
     * This a behaviour test when the labelling is not unique. This can be caused by multiple preferred single-cell QTs
     * or multiple preferred cell type labellings.
     */
    @Test
    public void testGetPreferredCellTypeLabellingWhenNonUnique() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2 );
        assertThat( qt.getIsPreferred() ).isFalse();
        assertThat( qt2.getIsPreferred() ).isTrue();

        // now we're going to do something really bad...
        qt.setIsPreferred( true );

        assertThatThrownBy( () -> scExpressionExperimentService.getPreferredCellTypeLabelling( ee ) )
                .isInstanceOf( NonUniqueResultException.class );
    }

    @Test
    public void testRecreateCellTypeFactor() {
        when( experimentalFactorService.create( any( ExperimentalFactor.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        ExperimentalFactor factor = scExpressionExperimentService.recreateCellTypeFactor( ee );
        assertThat( factor.getCategory() ).isNotNull().satisfies( f -> {
            assertThat( f.getCategory() ).isEqualTo( "cell type" );
            assertThat( f.getCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000324" );
        } );
    }

    private SingleCellDimension createSingleCellDimension() {
        SingleCellDimension scd = new SingleCellDimension();
        scd.setCellIds( IntStream.range( 0, 100 ).mapToObj( i -> RandomStringUtils.randomAlphanumeric( 10 ) ).collect( Collectors.toList() ) );
        scd.setNumberOfCells( 100 );
        int[] ct = new int[100];
        for ( int i = 0; i < ct.length; i++ ) {
            ct[i] = i < 50 ? 0 : 1;
        }
        CellTypeAssignment labelling = new CellTypeAssignment();
        labelling.setPreferred( true );
        labelling.setCellTypeIndices( ct );
        labelling.setCellTypes( Arrays.asList(
                Characteristic.Factory.newInstance( Categories.CELL_TYPE, "A", null ),
                Characteristic.Factory.newInstance( Categories.CELL_TYPE, "B", null ) ) );
        labelling.setNumberOfCellTypes( 2 );
        scd.getCellTypeAssignments().add( labelling );
        scd.getBioAssays().addAll( ee.getBioAssays() );
        scd.setBioAssaysOffset( new int[] { 0, 25, 50, 75 } );
        return scd;
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( boolean preferred ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.LOG2 );
        qt.setIsPreferred( preferred );
        sessionFactory.getCurrentSession().persist( qt );
        return createSingleCellVectors( createSingleCellDimension(), qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( QuantitationType qt ) {
        return createSingleCellVectors( createSingleCellDimension(), qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( SingleCellDimension singleCellDimension ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.LOG2 );
        qt.setIsPreferred( false );
        sessionFactory.getCurrentSession().persist( qt );
        return createSingleCellVectors( singleCellDimension, qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( SingleCellDimension scd, QuantitationType qt ) {
        Collection<SingleCellExpressionDataVector> vectors = new HashSet<>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            SingleCellExpressionDataVector v = new SingleCellExpressionDataVector();
            v.setDesignElement( cs );
            v.setSingleCellDimension( scd );
            v.setQuantitationType( qt );
            v.setData( new byte[8 * 100] );
            int[] ix = new int[100];
            for ( int i = 0; i < 100; i++ ) {
                ix[i] = i;
            }
            v.setDataIndices( ix );
            vectors.add( v );
        }
        return vectors;
    }
}
