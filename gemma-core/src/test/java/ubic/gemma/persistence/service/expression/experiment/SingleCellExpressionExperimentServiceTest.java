package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ThrowingConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataRemovedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExperimentalDesignUpdatedEvent;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
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
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVector;

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
        public ExperimentalFactorService experimentalFactorService( ExperimentalFactorDao experimentalFactorDao ) {
            return new ExperimentalFactorServiceImpl( experimentalFactorDao, mock(), mock() );
        }

        @Bean
        public ExperimentalFactorDao experimentalFactorDao( SessionFactory sessionFactory ) {
            return new ExperimentalFactorDaoImpl( sessionFactory );
        }

        @Bean
        public ExperimentalDesignService experimentalDesignService( ExperimentalDesignDao experimentalDesignDao ) {
            return new ExperimentalDesignServiceImpl( experimentalDesignDao );
        }

        @Bean
        public ExperimentalDesignDao experimentalDesignDao( SessionFactory sessionFactory ) {
            return new ExperimentalDesignDaoImpl( sessionFactory );
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public SingleCellSparsityMetrics singleCellSparsityMetrics() {
            return new SingleCellSparsityMetrics();
        }
    }

    @Autowired
    private SingleCellExpressionExperimentService scExpressionExperimentService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private SessionFactory sessionFactory;

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
    public void testGetSingleCellDimensionWithoutCellIds() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        assertThat( scd.getCellIds() ).isNotNull();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        assertThat( scExpressionExperimentService.streamCellIds( ee, qt, false ) )
                .hasSize( 100 );
        assertThat( scExpressionExperimentService.streamCellTypes( ee, scd.getCellTypeAssignments().iterator().next(), false ) )
                .hasSize( 100 );
        ThrowingConsumer<SingleCellDimension> t = scd2 -> {
            assertThat( scd2.getCellIds() ).isNull();
            assertThat( scd2.getNumberOfCells() ).isEqualTo( 100 );
            assertThat( scd2.getBioAssays() ).containsExactlyElementsOf( scd.getBioAssays() );
            assertThat( scd2.getNumberOfCellsBySample( 0 ) ).isEqualTo( 25 );
            assertThat( scd2.getNumberOfCellsBySample( 1 ) ).isEqualTo( 25 );
            assertThat( scd2.getNumberOfCellsBySample( 2 ) ).isEqualTo( 25 );
            assertThat( scd2.getNumberOfCellsBySample( 3 ) ).isEqualTo( 25 );
            assertThat( scd2.getCellTypeAssignments() ).hasSize( 1 );
            assertThat( scd2.getCellLevelCharacteristics() ).isEmpty();
        };
        assertThat( scExpressionExperimentService.getCellTypeAt( ee, qt, scd.getCellTypeAssignments().iterator().next().getId(), 0 ) )
                .isNotNull();
        assertThat( scExpressionExperimentService.getCellTypeAt( ee, qt, scd.getCellTypeAssignments().iterator().next().getId(), 0, 100 ) )
                .isNotNull()
                .hasSize( 100 );
        assertThat( scExpressionExperimentService.getCellTypeAt( ee, qt, "test", 0 ) )
                .isNotNull();
        assertThat( scExpressionExperimentService.getCellTypeAt( ee, qt, "test", 99 ) )
                .isNotNull();
        assertThat( scExpressionExperimentService.getCellTypeAt( ee, qt, "test", 0, 100 ) )
                .isNotNull()
                .hasSize( 100 );
        assertThatThrownBy( () -> scExpressionExperimentService.getCellTypeAt( ee, qt, "test", 100 ) )
                .isInstanceOf( IndexOutOfBoundsException.class );
        assertThatThrownBy( () -> scExpressionExperimentService.getCellTypeAt( ee, qt, "test", 100, 200 ) )
                .isInstanceOf( IndexOutOfBoundsException.class );
        assertThat( scExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt ) )
                .satisfies( t );
        assertThat( scExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee ) )
                .hasValueSatisfying( t );
        assertThat( scExpressionExperimentService.getSingleCellDimensionsWithoutCellIds( ee ) )
                .singleElement()
                .satisfies( t );
        assertThat( scExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt, true, true, true, true, false ) )
                .satisfies( t )
                .satisfies( scd2 -> {
                    assertThat( scd2.getCellTypeAssignments() )
                            .singleElement()
                            .satisfies( cta -> {
                                assertThat( cta.getId() ).isNotNull();
                                assertThat( cta.getCellTypes() ).hasSize( 2 );
                                assertThat( cta.getNumberOfCellTypes() ).isEqualTo( 2 );
                                assertThat( cta.getCellTypeIndices() ).isNull();
                                assertThat( cta.getProtocol() ).isNull();
                            } );
                } );
    }

    @Test
    public void testGetSingleCellDataMatrix() {
        RandomSingleCellDataUtils.setSeed( 123 );
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        SingleCellExpressionDataMatrix<?> matrix = scExpressionExperimentService.getSingleCellExpressionDataMatrix( ee, qt );
        assertThat( matrix.getQuantitationType() ).isEqualTo( qt );
        assertThat( matrix.getSingleCellDimension() ).isEqualTo( scd );
        assertThat( matrix.columns() ).isEqualTo( 100 );
        assertThat( matrix.rows() ).isEqualTo( 10 );
        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30 )
                .forEach( System.out::println );
        assertThat( scExpressionExperimentService.getNumberOfNonZeroes( ee, qt ) )
                .isEqualTo( 100L ); // 90% sparsity
        assertThat( scExpressionExperimentService.getNumberOfNonZeroesBySample( ee, qt, 30 ) )
                .containsOnlyKeys( ee.getBioAssays() )
                .containsValues( 26L, 24L, 23L, 27L );
    }

    @Test
    public void testGetSingleCellDataVectors() {
        RandomSingleCellDataUtils.setSeed( 123 );
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        scExpressionExperimentService.getSingleCellDataVectors( ee, qt );
        scExpressionExperimentService.getSingleCellDataVectors( ee, qt, true, true, true );
        scExpressionExperimentService.getSingleCellDataVectors( ee, qt, true, true, false );
        scExpressionExperimentService.getSingleCellDataVectors( ee, qt, true, false, true );
        scExpressionExperimentService.getSingleCellDataVectors( ee, qt, false, true, true );
        scExpressionExperimentService.getSingleCellDataVectors( ee, qt, false, false, false );

        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30 ).collect( Collectors.toList() );
        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30, true, true, true ).collect( Collectors.toList() );
        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30, true, true, false ).collect( Collectors.toList() );
        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30, true, false, true ).collect( Collectors.toList() );
        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30, false, true, true ).collect( Collectors.toList() );
        scExpressionExperimentService.streamSingleCellDataVectors( ee, qt, 30, false, false, false ).collect( Collectors.toList() );
    }

    @Test
    public void testAddSingleCellDataVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );

        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .contains( vectors.iterator().next().getQuantitationType() );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .allSatisfy( v -> assertThat( v.getId() ).isNotNull() );

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .singleElement()
                .satisfies( scd2 -> {
                    CellTypeAssignment cta = scd2.getCellTypeAssignments().iterator().next();
                    assertThat( cta.getCellType( 0 ) )
                            .isNotNull()
                            .satisfies( c -> assertThat( c.getValue() ).isEqualTo( "A" ) );
                    assertThat( cta.getCellType( 50 ) )
                            .isNotNull()
                            .satisfies( c -> assertThat( c.getValue() ).isEqualTo( "B" ) );
                } );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        SingleCellDimension scd2 = vectors2.iterator().next().getSingleCellDimension();
        assertThat( scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2, null ) )
                .isEqualTo( 10 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );
        assertThat( qt.getIsSingleCellPreferred() ).isFalse();

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .hasSize( 2 );

        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " [Preferred] with dimension " + scd + ".", ( String ) null );
        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt2 + " with dimension " + scd2 + ".", ( String ) null );
    }

    @Test
    public void testAddSingleCellDataVectorsWithInteger() {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.INT );
        qt.setIsSingleCellPreferred( true );
        sessionFactory.getCurrentSession().persist( qt );
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( qt );

        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .contains( vectors.iterator().next().getQuantitationType() );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .allSatisfy( v -> assertThat( v.getId() ).isNotNull() );

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .singleElement()
                .satisfies( scd2 -> {
                    CellTypeAssignment cta = scd2.getCellTypeAssignments().iterator().next();
                    assertThat( cta.getCellType( 0 ) )
                            .isNotNull()
                            .satisfies( c -> assertThat( c.getValue() ).isEqualTo( "A" ) );
                    assertThat( cta.getCellType( 50 ) )
                            .isNotNull()
                            .satisfies( c -> assertThat( c.getValue() ).isEqualTo( "B" ) );
                } );

        assertThat( scExpressionExperimentService.getSingleCellExpressionDataMatrix( ee, qt ) )
                .satisfies( matrix -> {
                    assertThat( matrix.getQuantitationType() ).isEqualTo( qt );
                } );

        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " with dimension " + scd + ".", ( String ) null );
    }

    @Test
    public void testAddSingleCellDataVectorsTwice() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThatThrownBy( () -> scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "already have vectors for the quantitation type" );
        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " with dimension " + scd + ".", ( String ) null );
    }

    @Test
    public void testAddSingleCellDataVectorsWhenThereIsAlreadyAPreferredSetOfVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        CellTypeAssignment ctl = scd.getCellTypeAssignments().iterator().next();
        ExperimentalFactor ctf = scExpressionExperimentService.getCellTypeFactor( ee ).orElseThrow( AssertionError::new );
        assertThat( ctf.getName() ).isEqualTo( "cell type" );
        assertThat( ctf.getDescription() ).isEqualTo( "Cell type factor pre-populated from " + ctl + "." );
        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        SingleCellDimension scd2 = vectors2.iterator().next().getSingleCellDimension();
        assertThat( scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2, null ) )
                .isEqualTo( 10 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 )
                .satisfiesOnlyOnce( quantitationType -> assertThat( quantitationType.getIsSingleCellPreferred() ).isTrue() );
        CellTypeAssignment ctl2 = scd2.getCellTypeAssignments().iterator().next();
        ExperimentalFactor ctf2 = scExpressionExperimentService.getCellTypeFactor( ee ).orElseThrow( AssertionError::new );
        assertThat( ctf2.getName() ).isEqualTo( "cell type" );
        assertThat( ctf2.getDescription() ).isEqualTo( "Cell type factor pre-populated from " + ctl2 + "." );
        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " [Preferred] with dimension " + scd + ".", ( String ) null );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "Created a cell type factor " + ctf + " from preferred cell type assignment " + ctl + "." );
        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt2 + " with dimension " + scd2 + ".", ( String ) null );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "Removed the cell type factor " + ctf + "." );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "Created a cell type factor " + ctf2 + " from preferred cell type assignment " + ctl2 + "." );
    }

    @Test
    public void testReplaceVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( qt );
        SingleCellDimension scd2 = vectors2.iterator().next().getSingleCellDimension();
        assertThat( scExpressionExperimentService.replaceSingleCellDataVectors( ee, qt, vectors2, null ) )
                .isEqualTo( 10 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .doesNotContainAnyElementsOf( vectors )
                .containsAll( vectors2 );

        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " with dimension " + scd + ".", ( String ) null );
        verify( auditTrailService ).addUpdateEvent( ee, DataReplacedEvent.class, "Replaced 10 vectors with 10 vectors for " + qt + " with dimension " + scd2 + "." );
    }

    @Test
    public void testRemoveVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        when( quantitationTypeService.reload( qt ) ).thenReturn( qt );
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( false );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        assertThat( scExpressionExperimentService.removeSingleCellDataVectors( ee, qt ) )
                .isEqualTo( 10 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        verify( auditTrailService )
                .addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " with dimension " + scd + ".", ( String ) null );
        verify( auditTrailService )
                .addUpdateEvent( ee, DataRemovedEvent.class, "Removed 10 vectors for " + qt + " with dimension " + scd + "." );
    }

    @Test
    public void testRemoveVectorsSharingADimension() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( scd );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( qt2 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        when( quantitationTypeService.reload( qt ) ).thenReturn( qt );

        scExpressionExperimentService.removeSingleCellDataVectors( ee, qt );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).doesNotContain( qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        verify( auditTrailService ).addUpdateEvent( ee, DataAddedEvent.class, "Added 10 vectors for " + qt + " with dimension " + scd + ".", ( String ) null );
        verify( auditTrailService ).addUpdateEvent( ee, DataRemovedEvent.class, "Removed 10 vectors for " + qt + " with dimension " + scd + "." );
    }

    @Test
    public void testRelabelCellTypes() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( scExpressionExperimentService.getCellTypeAssignments( ee ) )
                .hasSize( 1 );
        CellTypeAssignment cta = scExpressionExperimentService.getPreferredCellTypeAssignment( ee ).orElseThrow( AssertionError::new );
        String ctaS = cta.toString();
        assertThat( scExpressionExperimentService.getCellTypes( ee ) ).hasSize( 2 )
                .extracting( Characteristic::getValue )
                .containsExactlyInAnyOrder( "A", "B" );
        ExperimentalFactor ctf = scExpressionExperimentService.getCellTypeFactor( ee ).orElse( null );
        assertThat( ctf ).isNotNull();
        assertThat( ctf.getName() ).isEqualTo( "cell type" );
        assertThat( ctf.getDescription() ).isEqualTo( "Cell type factor pre-populated from " + cta + "." );
        String[] ct = new String[100];
        for ( int i = 0; i < ct.length; i++ ) {
            ct[i] = i < 75 ? "A" : "B";
        }
        CellTypeAssignment newLabelling = scExpressionExperimentService.relabelCellTypes( ee, qt, scd, Arrays.asList( ct ), null, null );
        String newLabellingS = newLabelling.toString();
        ExperimentalFactor newCtf = scExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( NullPointerException::new );
        assertThat( newLabelling ).satisfies( l -> {
            assertThat( l.getId() ).isNotNull();
            assertThat( l.isPreferred() ).isTrue();
        } );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .allSatisfy( v -> assertThat( v.getSingleCellDimension().getCellTypeAssignments() ).contains( newLabelling ) );
        assertThat( scExpressionExperimentService.getCellTypeAssignments( ee ) )
                .hasSize( 2 )
                .contains( newLabelling );
        assertThat( scExpressionExperimentService.getPreferredCellTypeAssignment( ee ) ).hasValue( newLabelling );
        assertThat( scExpressionExperimentService.getCellTypes( ee ) ).hasSize( 2 )
                .extracting( Characteristic::getValue )
                .containsExactlyInAnyOrder( "A", "B" );

        scExpressionExperimentService.removeCellTypeAssignment( ee, scd, newLabelling );
        assertThat( scExpressionExperimentService.getPreferredCellTypeAssignment( ee ) ).isEmpty();
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                "Created a cell type factor " + ctf + " from preferred cell type assignment " + ctaS + "." );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                "Removed the cell type factor " + ctf + "." );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                "Created a cell type factor " + newCtf + " from preferred cell type assignment " + newLabellingS + "." );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                "Removed the cell type factor " + newCtf + "." );
    }

    /**
     * This a behaviour test when the labelling is not unique. This can be caused by multiple preferred single-cell QTs
     * or multiple preferred cell type labellings.
     */
    @Test
    public void testGetPreferredCellTypeLabellingWhenNonUnique() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, null );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2, null );
        assertThat( qt.getIsSingleCellPreferred() ).isFalse();
        assertThat( qt2.getIsSingleCellPreferred() ).isTrue();

        // now we're going to do something really bad...
        qt.setIsSingleCellPreferred( true );

        assertThatThrownBy( () -> scExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .isInstanceOf( NonUniqueResultException.class );
    }

    @Test
    public void testRecreateCellTypeFactor() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors, null );
        CellTypeAssignment ctl = scExpressionExperimentService.getPreferredCellTypeAssignment( ee )
                .orElseThrow( AssertionError::new );
        assertThat( ee.getExperimentalDesign() ).isNotNull();
        ExperimentalFactor factor = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .filter( ef -> ef.getCategory() != null && CharacteristicUtils.hasCategory( ef.getCategory(), Categories.CELL_TYPE ) )
                .findFirst()
                .orElseThrow( AssertionError::new );
        ExperimentalFactor recreatedFactor = scExpressionExperimentService.recreateCellTypeFactor( ee );
        assertThat( recreatedFactor.getName() ).isEqualTo( "cell type" );
        assertThat( recreatedFactor.getDescription() ).isEqualTo( "Cell type factor pre-populated from " + ctl + "." );
        assertThat( recreatedFactor.getCategory() ).isNotNull().satisfies( f -> {
            assertThat( f.getCategory() ).isEqualTo( "cell type" );
            assertThat( f.getCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000324" );
        } );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "Created a cell type factor " + factor + " from preferred cell type assignment " + ctl + "." );
        verify( auditTrailService ).addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "Created a cell type factor " + recreatedFactor + " from preferred cell type assignment " + ctl + "." );
    }

    @Test
    public void testGetCellLevelCharacteristics() {
        SingleCellDimension dimension = createSingleCellDimension();
        int[] indices = new int[100];
        List<Characteristic> characteristics = new ArrayList<>();
        characteristics.add( Characteristic.Factory.newInstance( Categories.TREATMENT, "A", null ) );
        characteristics.add( Characteristic.Factory.newInstance( Categories.TREATMENT, "B", null ) );
        characteristics.add( Characteristic.Factory.newInstance( Categories.TREATMENT, "C", null ) );
        Random random = new Random( 123L );
        for ( int i = 0; i < 100; i++ ) {
            indices[i] = random.nextInt( characteristics.size() + 1 ) - 1;
        }
        CellLevelCharacteristics cellTreatments = CellLevelCharacteristics.Factory.newInstance( characteristics, indices );
        dimension.getCellLevelCharacteristics().add( cellTreatments );
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( dimension );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors, null );

        assertThat( scExpressionExperimentService.getCellLevelCharacteristics( ee ) )
                .hasSize( 2 )
                .extracting( clc -> clc.getCharacteristics().iterator().next().getCategory() )
                .containsExactlyInAnyOrder( "treatment", "cell type" );

        assertThat( scExpressionExperimentService.getCellLevelCharacteristics( ee, Categories.CELL_TYPE ) )
                .hasSize( 1 )
                .first()
                .satisfies( c -> {
                    assertThat( c.getCharacteristics() ).extracting( Characteristic::getValue )
                            .containsExactly( "A", "B" );
                } );

        assertThat( scExpressionExperimentService.getCellLevelCharacteristics( ee, Categories.TREATMENT ) )
                .hasSize( 1 )
                .first()
                .satisfies( c -> {
                    assertThat( c.getCharacteristics() ).extracting( Characteristic::getValue )
                            .containsExactly( "A", "B", "C" );
                } );

        assertThat( scExpressionExperimentService.getCellLevelCharacteristics( ee, Categories.GENOTYPE ) )
                .isEmpty();
    }

    @Test
    public void testUpdateSparsityMetrics() {
        Collection<SingleCellExpressionDataVector> vecs = createSingleCellVectors( false );
        QuantitationType qt = vecs.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vecs, null );
        assertThat( ee.getBioAssays() ).allSatisfy( ba -> {
            assertThat( ba.getNumberOfCells() ).isNull();
            assertThat( ba.getNumberOfDesignElements() ).isNull();
            assertThat( ba.getNumberOfCellsByDesignElements() ).isNull();
        } );
        qt.setIsSingleCellPreferred( true );
        scExpressionExperimentService.updateSparsityMetrics( ee );
        assertThat( ee.getBioAssays() ).allSatisfy( ba -> {
            assertThat( ba.getNumberOfCells() ).isNotNull();
            assertThat( ba.getNumberOfDesignElements() ).isNotNull();
            assertThat( ba.getNumberOfCellsByDesignElements() ).isNotNull();
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
        labelling.setName( "test" );
        labelling.setPreferred( true );
        labelling.setCellTypeIndices( ct );
        labelling.setNumberOfAssignedCells( 100 );
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
        qt.setIsSingleCellPreferred( preferred );
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
        qt.setIsSingleCellPreferred( false );
        sessionFactory.getCurrentSession().persist( qt );
        return createSingleCellVectors( singleCellDimension, qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( SingleCellDimension scd, QuantitationType qt ) {
        Collection<SingleCellExpressionDataVector> vectors = new HashSet<>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            vectors.add( randomSingleCellVector( ee, cs, qt, scd, 0.9 ) );
        }
        return vectors;
    }
}
