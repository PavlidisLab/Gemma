package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVector;

/**
 * Tests for the streaming overload of
 * {@link SingleCellExpressionExperimentService#addSingleCellDataVectors(ExpressionExperiment, QuantitationType, SingleCellDimension, Stream, String, boolean, boolean)}.
 * <p>
 * Mirrors the in-memory-H2 harness of {@link SingleCellExpressionExperimentServiceTest}, including its
 * mock beans for the read/write service split. These exercise the single-pass path added to stop
 * single-cell imports from materialising every vector before writing any of them.
 * <p>
 * The audit event is not asserted here. This context wires the impl directly with no AOP proxy, so the
 * {@code @Audited} advice never fires; aspect coverage lives in {@code AuditedAspectTest}.
 */
@ContextConfiguration
public class SingleCellStreamingAddTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class SingleCellStreamingAddTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return new SingleCellExpressionExperimentServiceImpl();
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        // EE DAO field-injects ArrayDesignDao for batched platform loads.
        @Bean
        public ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl( sessionFactory );
        }

        // SCEESI + EE DAO field-inject SingleCellDimensionExperimentDao.
        @Bean
        public SingleCellDimensionExperimentDao singleCellDimensionExperimentDao( SessionFactory sessionFactory ) {
            return new SingleCellDimensionExperimentDaoImpl( sessionFactory );
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
        public ExperimentalDesignReadService experimentalDesignReadService() {
            return mock( ExperimentalDesignReadService.class );
        }

        @Bean
        public ExperimentalFactorReadService experimentalFactorReadService() {
            return mock( ExperimentalFactorReadService.class );
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public SingleCellExperimentDesignAuditService singleCellExperimentDesignAuditService() {
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
    private SingleCellExperimentDesignAuditService singleCellExperimentDesignAuditService;

    @Autowired
    private SessionFactory sessionFactory;

    private ArrayDesign ad;
    private ExpressionExperiment ee;

    @BeforeEach
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

    @AfterEach
    public void resetMocks() {
        reset( auditTrailService );
        reset( singleCellExperimentDesignAuditService );
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

        // 100 cells across 4 BAs (25 each). With 90% sparsity the exact counts are data-dependent, but
        // they must be populated and internally consistent.
        assertThat( ee.getNumberOfCells() ).isNotNull().isPositive();
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertThat( ba.getNumberOfCells() ).isNotNull().isBetween( 0, 25 );
            assertThat( ba.getNumberOfDesignElements() ).isNotNull().isPositive();
            assertThat( ba.getNumberOfCellsByDesignElements() ).isNotNull().isNotNegative();
        }
        int sum = ee.getBioAssays().stream().mapToInt( BioAssay::getNumberOfCells ).sum();
        assertThat( ee.getNumberOfCells() ).isEqualTo( sum );
    }

    @Test
    public void testStreamingAddDoesNotApplySparsityForNonPreferredQt() {
        Fixture f = newFixture( "counts", false );
        service.addSingleCellDataVectors( ee, f.qt, f.scd, f.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

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
    public void testStreamingAddRejectsEmptyStream() {
        Fixture f = newFixture( "counts", true );
        // A valid SCD, but nothing in the stream: the count assertion must fire once it is drained.
        assertThatThrownBy( () -> service.addSingleCellDataVectors( ee, f.qt, f.scd, Stream.empty(), null, true, false ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testStreamingAddRejectsWrongQuantitationType() {
        Fixture f = newFixture( "counts", true );
        QuantitationType other = buildQt( "other", false );
        sessionFactory.getCurrentSession().persist( other );
        // Vectors reference f.qt, but `other` is passed as the target QT.
        assertThatThrownBy( () -> service.addSingleCellDataVectors( ee, other, f.scd, f.vectors.stream(), null, true, false ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testStreamingAddRejectsWrongSingleCellDimension() {
        Fixture f = newFixture( "counts", true );
        SingleCellDimension foreignScd = newDimension();
        // Vectors reference f.scd, but a different SCD instance is passed.
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
     * Single-pass consumption is the whole point of the streaming overload: if the implementation ever
     * materialises the source internally (a {@code toList()} slipped into the path, say), the memory
     * saving evaporates silently and only this test notices. The counting iterator records every pull.
     */
    @Test
    public void testStreamingAddConsumesSourceExactlyOnce() {
        Fixture f = newFixture( "counts", true );
        CountingIterator<SingleCellExpressionDataVector> counter = new CountingIterator<>( f.vectors.iterator() );
        Stream<SingleCellExpressionDataVector> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize( counter, 0 ), false );

        service.addSingleCellDataVectors( ee, f.qt, f.scd, stream, null, true, false );

        // hasNext() may be called somewhat more often than next() -- iterator-protocol overhead plus an
        // end-of-stream check -- but a re-iteration would roughly double both counts.
        assertThat( counter.elementsPulled() ).isEqualTo( f.vectors.size() );
        assertThat( counter.hasNextCalls() ).isBetween( f.vectors.size(), f.vectors.size() + 3 );
    }

    /**
     * Covers the {@code scdJustCreated} branch of the streaming validator: re-adding vectors against a
     * dimension that is already wired to existing vectors must be allowed.
     */
    @Test
    public void testStreamingAddAcceptsPersistentDimensionFromPriorAdd() {
        Fixture first = newFixture( "counts", true );
        service.addSingleCellDataVectors( ee, first.qt, first.scd, first.vectors.stream(), null, true, false );
        sessionFactory.getCurrentSession().flush();

        // Second QT, reusing the persisted dimension. The impl skips quantitationTypeService.create()
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

    /** Records iterator usage so the single-pass invariant can be asserted rather than assumed. */
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
