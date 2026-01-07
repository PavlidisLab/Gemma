package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.hibernate.Hibernate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.util.test.BaseIntegrationTest;
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
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils.randomExpressionMatrix;

public class ProcessedExpressionDataVectorCreationHelperServiceTest extends BaseIntegrationTest {

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

    @Test
    public void testCreateProcessedDataVectors() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new LogNormalDistribution( 9, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LINEAR, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        QuantitationType processedQt = processedExpressionDataVectorCreationHelperService.createProcessedDataVectors( ee, false, summary );
        assertEquals( 100, summary.getNumberOfDataVectors() );
        assertEquals( "log2cpm - Processed version", processedQt.getName() );
        assertEquals( GeneralType.QUANTITATIVE, processedQt.getGeneralType() );
        assertEquals( StandardQuantitationType.AMOUNT, processedQt.getType() );
        assertEquals( ScaleType.LOG2, processedQt.getScale() );
        assertEquals( PrimitiveType.DOUBLE, processedQt.getRepresentation() );
        assertTrue( processedQt.getIsMaskedPreferred() );
        assertEquals( ( Integer ) NUM_PROBES, ee.getNumberOfDataVectors() );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 ) // one raw and one processed
                .contains( processedQt );
    }

    @Test
    public void testCreateProcessedDataVectorsFromLog2Data() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new NormalDistribution( 15, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        processedExpressionDataVectorCreationHelperService.createProcessedDataVectors( ee, false, summary );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );
    }

    @Test
    public void testCreateProcessedDataVectorsFromLog2RatiometricData() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        processedExpressionDataVectorCreationHelperService.createProcessedDataVectors( ee, false, summary );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );
    }

    @Test
    public void testThaw() throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 8, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        processedExpressionDataVectorCreationHelperService.createProcessedDataVectors( ee, false, summary );
        assertEquals( NUM_PROBES, summary.getNumberOfDataVectors() );

        Collection<ProcessedExpressionDataVector> reloadedVectors;

        // thaw a single vector
        reloadedVectors = processedExpressionDataVectorDao.getProcessedDataVectors( ee );
        ProcessedExpressionDataVector oneVector = reloadedVectors.iterator().next();
        checkVectorInitializationBeforeThaw( oneVector );
        oneVector = processedExpressionDataVectorDao.thaw( oneVector );
        checkVectorInitializationAfterThaw( oneVector );

        // thaw all vectors in bulk
        reloadedVectors = processedExpressionDataVectorDao.getProcessedDataVectors( ee );
        assertThat( reloadedVectors ).allSatisfy( ProcessedExpressionDataVectorCreationHelperServiceTest::checkVectorInitializationBeforeThaw );
        reloadedVectors = processedExpressionDataVectorDao.thaw( reloadedVectors );
        assertThat( reloadedVectors )
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

        return ee;
    }
}
