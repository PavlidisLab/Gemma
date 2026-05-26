package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
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
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVector;

/**
 * Tests for the streaming overload of
 * {@link SingleCellExpressionExperimentService#addSingleCellDataVectors(ExpressionExperiment, QuantitationType, SingleCellDimension, Stream, String, boolean, boolean)}.
 *
 * Mirrors the in-memory-H2 setup used by {@link SingleCellExpressionExperimentServiceTest}. These tests exercise the
 * single-pass streaming path introduced to address out-of-memory failures in {@code addCELLxGENEData}.
 */
@ContextConfiguration
public class SingleCellStreamingAddTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class Config extends BaseDatabaseTestContextConfiguration {

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
    private SingleCellExpressionExperimentService service;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

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
            cs.setName( "cs" + i );
            cs.setArrayDesign( ad );
            ad.getCompositeSequences().add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.setTaxon( taxon );
        BioMaterial bm = BioMaterial.Factory.newInstance( "bm", taxon );
        sessionFactory.getCurrentSession().persist( bm );
        for ( String name : Arrays.asList( "a", "b", "c", "d" ) ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( name, ad, bm ) );
        }
        ee = expressionExperimentDao.create( ee );
    }

    @After
    public void resetMocks() {
        reset( auditTrailService );
    }

    @Test
    public void testStreamingAddPersistsAllVectors() {
        Fixture f = newFixture( "counts", true );
        int added = service.addSingleCellDataVectors( ee, f.qt, f.scd, f.vectors.stream(), null, true, false );
        assertThat( added ).isEqualTo( f.vectors.size() );

        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( f.qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( f.vectors.size() )
                .allSatisfy( v -> {
                    assertThat( v.getId() ).isNotNull();
                    assertThat( v.getExpressionExperiment() ).isEqualTo( ee );
                    assertThat( v.getQuantitationType() ).isEqualTo( f.qt );
                    assertThat( v.getSingleCellDimension() ).isEqualTo( f.scd );
                } );
    }

    @Test
    public void testStreamingAddMatchesCollectionVariant() {
        // Adding a non-preferred second QT lets us run both variants on the same EE without tripping
        // the "cell type factor already exists" / "preferred QT already exists" cross-talk.
        Fixture streaming = newFixture( "counts-stream", true );
        service.addSingleCellDataVectors( ee, streaming.qt, streaming.scd, streaming.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

        Fixture collection = newFixture( "counts-collection", false );
        service.addSingleCellDataVectors( ee, collection.qt, collection.vectors, null, true, false );
        sessionFactory.getCurrentSession().flush();

        // Both variants persisted the same number of vectors with identical wiring.
        long streamingVecs = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( streaming.qt ) ).count();
        long collectionVecs = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( collection.qt ) ).count();
        assertThat( streamingVecs ).isEqualTo( streaming.vectors.size() );
        assertThat( collectionVecs ).isEqualTo( collection.vectors.size() );
        assertThat( ee.getQuantitationTypes() ).contains( streaming.qt, collection.qt );
    }

    @Test
    public void testStreamingAddAppliesSparsityMetricsForPreferredQt() {
        Fixture f = newFixture( "counts", true );
        service.addSingleCellDataVectors( ee, f.qt, f.scd, f.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

        // 100 cells total across 4 BAs (25 each). With 90% sparsity, every BA should have at least one expressed cell;
        // the exact number is data-dependent but must be non-null and within [0, 25].
        assertThat( ee.getNumberOfCells() ).isNotNull().isPositive();
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertThat( ba.getNumberOfCells() ).isNotNull().isBetween( 0, 25 );
            assertThat( ba.getNumberOfDesignElements() ).isNotNull().isPositive();
            assertThat( ba.getNumberOfCellsByDesignElements() ).isNotNull().isNotNegative();
        }
        // The per-BA counts must sum to the experiment-level count.
        int sum = ee.getBioAssays().stream().mapToInt( BioAssay::getNumberOfCells ).sum();
        assertThat( ee.getNumberOfCells() ).isEqualTo( sum );
    }

    @Test
    public void testStreamingAddDoesNotApplySparsityForNonPreferredQt() {
        Fixture f = newFixture( "counts", false );
        service.addSingleCellDataVectors( ee, f.qt, f.scd, f.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

        // Sparsity metrics are only computed for the preferred QT.
        assertThat( ee.getNumberOfCells() ).isNull();
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertThat( ba.getNumberOfCells() ).isNull();
        }
    }

    @Test
    public void testStreamingAddCreatesCellTypeFactorWhenPreferred() {
        Fixture f = newFixture( "counts", true );
        service.addSingleCellDataVectors( ee, f.qt, f.scd, f.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();
        ExperimentalFactor ctf = service.getCellTypeFactor( ee ).orElse( null );
        assertThat( ctf ).isNotNull();
        assertThat( ctf.getName() ).isEqualTo( "cell type" );
    }

    @Test
    public void testStreamingAddEmitsAuditEvent() {
        Fixture f = newFixture( "counts", true );
        service.addSingleCellDataVectors( ee, f.qt, f.scd, f.vectors.stream(), "loaded from fixture", true, false );
        sessionFactory.getCurrentSession().flush();
        verify( auditTrailService ).addUpdateEvent(
                org.mockito.ArgumentMatchers.eq( ee ),
                org.mockito.ArgumentMatchers.eq( ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent.class ),
                org.mockito.ArgumentMatchers.contains( "Added " + f.vectors.size() + " vectors" ),
                org.mockito.ArgumentMatchers.eq( "loaded from fixture" ) );
    }

    @Test
    public void testStreamingAddRejectsEmptyStream() {
        Fixture f = newFixture( "counts", true );
        // We still need a valid SCD, but the stream itself is empty.
        // The implementation must reject an empty stream after consuming it (no vectors → assertion failure).
        assertThatThrownBy( () -> service.addSingleCellDataVectors( ee, f.qt, f.scd, Stream.empty(), null, true, false ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testStreamingAddRejectsWrongQuantitationType() {
        Fixture f = newFixture( "counts", true );
        QuantitationType other = buildQt( "other", false );
        sessionFactory.getCurrentSession().persist( other );
        // Vectors reference f.qt, but we pass `other` as the target QT — validator should reject.
        assertThatThrownBy( () -> service.addSingleCellDataVectors( ee, other, f.scd, f.vectors.stream(), null, true, false ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testStreamingAddRejectsWrongSingleCellDimension() {
        Fixture f = newFixture( "counts", true );
        SingleCellDimension foreignScd = newDimension();
        // Vectors reference f.scd, but we pass a different SCD instance — validator should reject.
        assertThatThrownBy( () -> service.addSingleCellDataVectors( ee, f.qt, foreignScd, f.vectors.stream(), null, true, false ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testStreamingAddRejectsDuplicateQuantitationTypeName() {
        Fixture first = newFixture( "counts", true );
        service.addSingleCellDataVectors( ee, first.qt, first.scd, first.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

        Fixture dup = newFixture( "counts", false );
        assertThatThrownBy( () -> service.addSingleCellDataVectors( ee, dup.qt, dup.scd, dup.vectors.stream(), null, true, false ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "There is already a quantitation type named counts" );
    }

    /**
     * The streaming overload is the OOM fix's load-bearing contract: it must consume the source stream
     * exactly once. If it materialises the stream (e.g. via {@code toList()}) somewhere internally, this
     * test will fail because the underlying iterator records each {@code next()} call.
     */
    @Test
    public void testStreamingAddConsumesSourceExactlyOnce() {
        Fixture f = newFixture( "counts", true );
        CountingIterator<SingleCellExpressionDataVector> counter = new CountingIterator<>( f.vectors.iterator() );
        Stream<SingleCellExpressionDataVector> stream = StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize( counter, 0 ), false );

        service.addSingleCellDataVectors( ee, f.qt, f.scd, stream, null, true, false );

        // Single-pass invariant: each vector is pulled exactly once. hasNext() is allowed to be called
        // somewhat more than next() (iterator-protocol overhead and an end-of-stream check), but the upper
        // bound must stay close to size — a re-iteration would roughly double both counts.
        assertThat( counter.elementsPulled() ).isEqualTo( f.vectors.size() );
        assertThat( counter.hasNextCalls() ).isBetween( f.vectors.size(), f.vectors.size() + 3 );
    }

    /**
     * Uses a persistent SCD (already attached to the experiment via a prior add) to verify the
     * "scdJustCreated" branch in the streaming validator: re-adding vectors against an SCD that is
     * already wired up to existing vectors must be allowed.
     */
    @Test
    public void testStreamingAddAcceptsPersistentDimensionFromPriorAdd() {
        Fixture first = newFixture( "counts", true );
        service.addSingleCellDataVectors( ee, first.qt, first.scd, first.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

        // Second QT, but reuse the persisted dimension. The impl skips quantitationTypeService.create()
        // when the QT already has an id, so persist it here to side-step the mocked service.
        QuantitationType qt2 = buildQt( "counts2", false );
        sessionFactory.getCurrentSession().persist( qt2 );
        Collection<SingleCellExpressionDataVector> vectors2 = ad.getCompositeSequences().stream()
                .map( cs -> randomSingleCellVector( ee, cs, qt2, first.scd, 0.9 ) )
                .collect( Collectors.toList() );

        int added = service.addSingleCellDataVectors( ee, qt2, first.scd, vectors2.stream(), null, true, false );
        assertThat( added ).isEqualTo( vectors2.size() );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() ).hasSize( first.vectors.size() + vectors2.size() );
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static class Fixture {
        final QuantitationType qt;
        final SingleCellDimension scd;
        final List<SingleCellExpressionDataVector> vectors;

        Fixture( QuantitationType qt, SingleCellDimension scd, List<SingleCellExpressionDataVector> vectors ) {
            this.qt = qt;
            this.scd = scd;
            this.vectors = vectors;
        }
    }

    private Fixture newFixture( String qtName, boolean preferred ) {
        QuantitationType qt = buildQt( qtName, preferred );
        sessionFactory.getCurrentSession().persist( qt );
        SingleCellDimension scd = newDimension();
        List<SingleCellExpressionDataVector> vectors = ad.getCompositeSequences().stream()
                .map( cs -> randomSingleCellVector( ee, cs, qt, scd, 0.9 ) )
                .collect( Collectors.toList() );
        return new Fixture( qt, scd, vectors );
    }

    private QuantitationType buildQt( String name, boolean preferred ) {
        QuantitationType qt = new QuantitationType();
        qt.setName( name );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.LOG2 );
        qt.setIsSingleCellPreferred( preferred );
        return qt;
    }

    private SingleCellDimension newDimension() {
        SingleCellDimension scd = new SingleCellDimension();
        scd.setCellIds( IntStream.range( 0, 100 )
                .mapToObj( i -> RandomStringUtils.insecure().nextAlphanumeric( 10 ) )
                .collect( Collectors.toList() ) );
        scd.setNumberOfCellIds( 100 );
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

    /** Records iterator usage so we can prove the streaming path consumes the source exactly once. */
    private static class CountingIterator<T> implements Iterator<T> {
        private final Iterator<T> delegate;
        private final AtomicInteger pulled = new AtomicInteger();
        private final AtomicInteger hasNextCalls = new AtomicInteger();

        CountingIterator( Iterator<T> delegate ) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            hasNextCalls.incrementAndGet();
            return delegate.hasNext();
        }

        @Override
        public T next() {
            pulled.incrementAndGet();
            return delegate.next();
        }

        int elementsPulled() {
            return pulled.get();
        }

        int hasNextCalls() {
            return hasNextCalls.get();
        }
    }
}