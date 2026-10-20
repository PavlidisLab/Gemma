package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.core.util.test.ThawTestUtils;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils.randomExpressionMatrix;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils.setSeed;

public class ProcessedExpressionDataVectorCreationHelperServiceTest extends BaseIntegrationTest5 {

    private static final int NUM_PROBES = 100;

    @Autowired
    private ProcessedExpressionDataVectorCreationHelperService processedExpressionDataVectorCreationHelperService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorDao;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    private SessionFactory sessionFactory;

    /**
     * Read, normalize, write — the three steps that used to be one {@code createProcessedDataVectors} call.
     * <p>
     * 🛑 Calling them from here is the point of this test, not a convenience. {@link BaseIntegrationTest5} is
     * deliberately NOT {@code @Transactional}, so each step really does open and commit its own transaction and
     * the carrier really is detached in between. Wrapping this in one transaction would hide every
     * detached-entity failure the split can cause, which is the only kind it can cause.
     */
    private QuantitationType createProcessedDataVectors( ExpressionExperiment ee, boolean ignoreQuantitationMismatch,
            ProcessedExpressionDataVectorCreationSummary summary ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        ComputedProcessedData computed = processedExpressionDataVectorCreationHelperService
                .readProcessedDataInputs( ee, ignoreQuantitationMismatch, summary, true );
        processedExpressionDataVectorCreationHelperService.normalizeProcessedData( computed, summary );
        return processedExpressionDataVectorCreationHelperService.replaceProcessedDataVectors( ee, computed, summary );
    }

    @Test
    public void testCreateProcessedDataVectors() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        setSeed( 123L );
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new LogNormalDistribution( 9, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LINEAR, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        QuantitationType processedQt = createProcessedDataVectors( ee, false, summary );
        assertEquals( 100, summary.getNumberOfDataVectors() );
        assertEquals( "log2cpm - Processed version", processedQt.getName() );
        assertEquals( GeneralType.QUANTITATIVE, processedQt.getGeneralType() );
        assertEquals( StandardQuantitationType.AMOUNT, processedQt.getType() );
        assertEquals( ScaleType.LOG2, processedQt.getScale() );
        assertEquals( PrimitiveType.DOUBLE, processedQt.getRepresentation() );
        assertTrue( processedQt.getIsMaskedPreferred() );
        // createProcessedDataVectors mutates the managed instance, not this method's `ee`.
        // Reload to see the updated numberOfDataVectors and quantitation-types collection.
        ee = expressionExperimentService.thaw( expressionExperimentService.load( ee.getId() ) );
        assertEquals( ( Integer ) NUM_PROBES, ee.getNumberOfDataVectors() );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 ) // one raw and one processed
                .contains( processedQt );
    }

    @Test
    public void testCreateProcessedDataVectorsFromLog2Data() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        setSeed( 123L );
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new NormalDistribution( 15, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        createProcessedDataVectors( ee, false, summary );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );
    }

    @Test
    public void testCreateProcessedDataVectorsFromLog2RatiometricData() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        createProcessedDataVectors( ee, false, summary );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );
    }

    /**
     * A row with no value in any sample survives quantile normalization untouched. The normalizer drops such rows
     * before it ranks, so its output has fewer rows than its input; writing the result back by the input's row count
     * ran off the end. frinkbro hit it on GSE21509 (eid 30208, 2026-09-14) through {@code corrMat -force}:
     * {@code Index 45708 out of bounds for length 45708} against 46,628 design elements.
     * <p>
     * 4,000 rows is the smallest size that is normalized at all.
     */
    @Test
    public void testCreateProcessedDataVectorsWithARowMissingInEverySample() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        setSeed( 123L );
        int numProbes = 4000;
        double[][] matrix = randomExpressionMatrix( numProbes, 4, new NormalDistribution( 10, 1 ) );
        Arrays.fill( matrix[0], Double.NaN );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, false );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();

        createProcessedDataVectors( ee, false, summary );

        assertTrue( summary.isQuantileNormalized() );
        assertEquals( numProbes, summary.getNumberOfDataVectors() );
        ee = expressionExperimentService.thaw( expressionExperimentService.load( ee.getId() ) );
        assertThat( ee.getProcessedExpressionDataVectors() )
                .filteredOn( v -> v.getDesignElement().getName().equals( "cs0" ) )
                .singleElement()
                .satisfies( v -> assertThat( v.getDataAsDoubles() ).containsOnly( Double.NaN ) );
    }

    /**
     * The sample-correlation matrix is built from what {@code readProcessedDataInputs} returns, with no
     * transaction open, and {@code AffyProbeNameFilter} then reads each design element's sequence name. The
     * sequences must leave the read initialized: GSE96826 (ee 15112) failed with a
     * {@code LazyInitializationException} on a {@link BioSequence} proxy at exactly that point.
     */
    @Test
    public void testReadProcessedDataInputsInitializesDesignElementSequences() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        setSeed( 123L );
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new LogNormalDistribution( 9, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LINEAR, false, true );
        ComputedProcessedData computed = processedExpressionDataVectorCreationHelperService
                .readProcessedDataInputs( ee, false, new ProcessedExpressionDataVectorCreationSummary(), false );
        assertThat( computed.getData().keySet() )
                .hasSize( NUM_PROBES )
                .allSatisfy( cs -> {
                    assertThat( Hibernate.isInitialized( cs.getBiologicalCharacteristic() ) ).isTrue();
                    assertThat( cs.getBiologicalCharacteristic().getName() ).startsWith( "seq" );
                } );
    }

    @Test
    public void testThaw() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        setSeed( 123L );
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 8, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        createProcessedDataVectors( ee, false, summary );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );

        Long eeId = ee.getId();

        // Reload vectors in a fresh session so the expressionExperiment association
        // is a lazy proxy (otherwise it resolves to the already-managed ee in the
        // test session, and Hibernate.isInitialized(...) returns true before thaw).
        // The fetch profile mirrors ProcessedExpressionDataVectorDaoImpl.getProcessedVectors
        // so designElement / arrayDesign / biologicalCharacteristic are already realised.
        Collection<ProcessedExpressionDataVector> detachedVectors = ThawTestUtils.queryDetachedInFreshSession( sessionFactory, session -> {
            //noinspection unchecked
            return ( Collection<ProcessedExpressionDataVector> ) ( Collection<?> ) session.createQuery(
                            "select dedv from ProcessedExpressionDataVector dedv "
                                    + "join fetch dedv.designElement cs "
                                    + "join fetch cs.arrayDesign "
                                    + "left join fetch cs.biologicalCharacteristic "
                                    + "join fetch dedv.bioAssayDimension "
                                    + "left join fetch dedv.quantitationType "
                                    + "where dedv.expressionExperiment.id = :eeId" )
                    .setParameter( "eeId", eeId )
                    .list();
        } );
        assertThat( detachedVectors ).hasSize( NUM_PROBES );

        // thaw a single vector — pass the detached vector to the service, which
        // re-attaches it via ensureInSession() inside its own read-only tx and
        // initializes the lazy associations.
        ProcessedExpressionDataVector oneVector = detachedVectors.iterator().next();
        checkVectorInitializationBeforeThaw( oneVector );
        oneVector = processedExpressionDataVectorDao.thaw( oneVector );
        checkVectorInitializationAfterThaw( oneVector );

        // thaw all vectors in bulk — re-fetch detached set so the "before" assertion
        // is meaningful again (the singleton thaw above already mutated one of them).
        Collection<ProcessedExpressionDataVector> bulkDetachedVectors = ThawTestUtils.queryDetachedInFreshSession( sessionFactory, session -> {
            //noinspection unchecked
            return ( Collection<ProcessedExpressionDataVector> ) ( Collection<?> ) session.createQuery(
                            "select dedv from ProcessedExpressionDataVector dedv "
                                    + "join fetch dedv.designElement cs "
                                    + "join fetch cs.arrayDesign "
                                    + "left join fetch cs.biologicalCharacteristic "
                                    + "join fetch dedv.bioAssayDimension "
                                    + "left join fetch dedv.quantitationType "
                                    + "where dedv.expressionExperiment.id = :eeId" )
                    .setParameter( "eeId", eeId )
                    .list();
        } );
        assertThat( bulkDetachedVectors ).allSatisfy( ProcessedExpressionDataVectorCreationHelperServiceTest::checkVectorInitializationBeforeThaw );
        Collection<ProcessedExpressionDataVector> thawedVectors = processedExpressionDataVectorDao.thaw( bulkDetachedVectors );
        assertThat( thawedVectors )
                .allSatisfy( ProcessedExpressionDataVectorCreationHelperServiceTest::checkVectorInitializationAfterThaw );
    }

    private static void checkVectorInitializationBeforeThaw( ProcessedExpressionDataVector vector ) {
        assertThat( Hibernate.isInitialized( vector ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getExpressionExperiment() ) ).isFalse();
        assertThat( Hibernate.isInitialized( vector.getBioAssayDimension() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement().getBiologicalCharacteristic() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getQuantitationType() ) ).isTrue();
    }

    private static void checkVectorInitializationAfterThaw( ProcessedExpressionDataVector vector ) {
        assertThat( Hibernate.isInitialized( vector ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getExpressionExperiment() ) ).isTrue();
        assertThat( vector.getExpressionExperiment().getBioAssays() )
                .allMatch( Hibernate::isInitialized );
        assertThat( vector.getExpressionExperiment().getBioAssays() ).allSatisfy( ba -> {
            assertThat( Hibernate.isInitialized( ba.getSampleUsed() ) ).isTrue();
            assertThat( Hibernate.isInitialized( ba.getSampleUsed().getFactorValues() ) ).isTrue();
            assertThat( Hibernate.isInitialized( ba.getArrayDesignUsed() ) ).isTrue();
            assertThat( Hibernate.isInitialized( ba.getOriginalPlatform() ) ).isTrue();
        } );
        assertThat( Hibernate.isInitialized( vector.getBioAssayDimension() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement().getArrayDesign() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement().getBiologicalCharacteristic() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getQuantitationType() ) ).isTrue();
    }


    private ExpressionExperiment getTestExpressionExperimentForRawExpressionMatrix( double[][] matrix, ScaleType scaleType, boolean isRatio ) {
        return getTestExpressionExperimentForRawExpressionMatrix( matrix, scaleType, isRatio, false );
    }

    private ExpressionExperiment getTestExpressionExperimentForRawExpressionMatrix( double[][] matrix, ScaleType scaleType, boolean isRatio, boolean withSequences ) {
        ExpressionExperiment ee = new ExpressionExperiment();

        Taxon taxon = new Taxon();
        taxon.setCommonName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        taxon = taxonService.create( taxon );

        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        List<CompositeSequence> probes = new ArrayList<>();
        for ( int i = 0; i < matrix.length; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "cs" + i );
            cs.setArrayDesign( ad );
            if ( withSequences ) {
                BioSequence bs = new BioSequence();
                bs.setName( "seq" + i );
                bs.setTaxon( taxon );
                cs.setBiologicalCharacteristic( bioSequenceService.create( bs ) );
            }
            ad.getCompositeSequences().add( cs );
            probes.add( cs );
        }
        ad = arrayDesignService.create( ad );

        List<BioMaterial> bioMaterials = new ArrayList<>();
        for ( int i = 0; i < matrix[0].length; i++ ) {
            BioMaterial bm = new BioMaterial();
            bm.setSourceTaxon( taxon );
            bm = bioMaterialService.create( bm );
            bioMaterials.add( bm );
        }
        List<BioAssay> bas = new ArrayList<>();
        for ( int i = 0; i < matrix[0].length; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setSampleUsed( bioMaterials.get( i ) );
            // ba = bioAssayService.create( ba );
            bas.add( ba );
        }
        ee.getBioAssays().addAll( bas );

        ee = expressionExperimentService.create( ee );

        QuantitationType qt = new QuantitationType();
        qt.setName( scaleType == ScaleType.COUNT ? "counts" : "log2cpm" );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( scaleType );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( scaleType == ScaleType.COUNT ? StandardQuantitationType.COUNT : StandardQuantitationType.AMOUNT );
        qt.setIsRatio( isRatio );
        qt.setIsPreferred( true );

        BioAssayDimension bad = new BioAssayDimension();
        bad.setBioAssays( bas );

        Set<RawExpressionDataVector> vectors = new HashSet<>();
        int i = 0;
        for ( double[] row : matrix ) {
            RawExpressionDataVector ev = new RawExpressionDataVector();
            ev.setExpressionExperiment( ee );
            ev.setQuantitationType( qt );
            ev.setBioAssayDimension( bad );
            ev.setDesignElement( probes.get( i ) );
            ev.setDataAsDoubles( row );
            vectors.add( ev );
            i++;
        }

        expressionExperimentService.addRawDataVectors( ee, qt, vectors );

        // addRawDataVectors mutates the managed instance (re-fetched via ensureEeInSession),
        // not this method's `ee` parameter. With L2 cache disabled (BaseDatabaseTest5) the
        // test's local reference doesn't see the new raw vectors / QT, so reload to expose
        // them on the returned ee. thaw() pulls in the raw vector bag.
        ee = expressionExperimentService.load( ee.getId() );
        ee = expressionExperimentService.thaw( ee );
        return ee;
    }
}
