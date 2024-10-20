/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.loader.expression.geo.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixBuilder;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Geeq;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

/**
 * Test full procedure of loading GEO data, focus on corner cases. Tests deletion of data sets as well.
 *
 * @author pavlidis
 */
@Category({ GeoTest.class, SlowTest.class })
public class GeoDatasetServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private TwoChannelMissingValues twoChannelMissingValues;
    @Autowired
    private ExpressionDataFileService dataFileService;
    @Autowired
    private AclTestUtils aclTestUtils;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    @Autowired
    private ProcessedExpressionDataVectorService dataVectorService;
    @Autowired
    private GeeqService geeqService;
    @Autowired
    private PreprocessorService preprocessorService;

    private Collection<ExpressionExperiment> ees;
    private ExpressionExperiment ee;

    @Before
    public void setUp() throws URISyntaxException {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
    }

    private void setUpDatasetFromGeo( String geoAccession ) {
        try {
            Collection<?> results = geoService.fetchAndLoad( geoAccession, false, true, false );
            ees = ( Collection<ExpressionExperiment> ) results;
        } catch ( AlreadyExistsInSystemException e ) {
            ees = ( Collection<ExpressionExperiment> ) e.getData();
            assumeNoException( String.format( "%s is already loaded in the database.", geoAccession ), e );
        }
        ee = ees.iterator().next();
    }

    @After
    public void tearDown() {
        if ( ees != null ) {
            eeService.remove( ees );
        }
    }

    /*
     * Has multiple species (mouse and human, one and two platforms respectively), also test publication entry.
     */
    @Test
    @Category(SlowTest.class)
    public void testFetchAndLoadGSE1133() throws Exception {
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse1133Short" ) ) );
        setUpDatasetFromGeo( "GSE1133" );

        assertEquals( 2, this.ees.size() );

        for ( ExpressionExperiment e : this.ees ) {
            e = eeService.thawLite( e );

            aclTestUtils.checkEEAcls( e );

            assertNotNull( e.getPrimaryPublication() );
            assertEquals( "6062-7", e.getPrimaryPublication().getPages() );
        }

    }

    @Test
    public void testFetchAndLoadGSE37646RNASEQ() {
        setUpDatasetFromGeo( "GSE37646" );
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchAndLoadGSE16035() {
        setUpDatasetFromGeo( "GSE16035" );

        preprocessorService.process( ee );

        ee = eeService.thaw( ee );
        Collection<ProcessedExpressionDataVector> vecs = ee.getProcessedExpressionDataVectors();
        vecs = dataVectorService.thaw( vecs );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vecs );

        ExpressionDataMatrix<Double> matrix = builder.getProcessedData();
        double a = matrix.get( 0, 0 );
        double b = matrix.get( 0, 1 );
        assertTrue( a - b != 0.0 );
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchAndLoadGSE12135EXON() {
        setUpDatasetFromGeo( "GSE12135" );
        aclTestUtils.checkEEAcls( ee );
    }

    /*
     * Left out quantitation types due to bug in how quantitation types were cached during persisting, if the QTs didn't
     * have descriptions. Converted into a test of taxon filtering.
     */
    @Test(expected = IllegalStateException.class)
    public void testFetchAndLoadGSE13657() {
        setUpDatasetFromGeo( "GSE13657" );
        fail( "Expected an exception for no supported taxa" );

        // part of original test, for checking QT inclusion
        //        ee = this.eeService.thawLite( ee );
        //        aclTestUtils.checkEEAcls( ee );
        //        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        //        assertEquals( 13, qts.size() );
        //
        //        // make sure we got characteristics and treatments for both channels.
        //        for ( BioAssay ba : ee.getBioAssays() ) {
        //
        //            BioMaterial bm = ba.getSampleUsed();
        //
        //            assertNotNull( bm );
        //
        //            log.info( bm + " " + bm.getDescription() );
        //
        //            assertEquals( 9, bm.getCharacteristics().size() );
        //
        //        }

    }

    @Test
    @Category(SlowTest.class)
    public void testFetchAndLoadGSE9048() throws Exception {
        setUpDatasetFromGeo( "GSE9048" );

        ee = eeService.load( ee.getId() );
        assertNotNull( ee );
        ee = this.eeService.thawLite( ee );

        // fix for unknown log scale
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getIsPreferred() ) {
                qt.setScale( ScaleType.LOG2 );
                quantitationTypeService.update( qt );
            }
        }

        aclTestUtils.checkEEAcls( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        assertEquals( 16, qts.size() );

        twoChannelMissingValues.computeMissingValues( ee );

        ee = eeService.load( ee.getId() );
        assertNotNull( ee );
        ee = this.eeService.thawLite( ee );
        qts = eeService.getQuantitationTypes( ee );
        assertEquals( 17, qts.size() ); // 16 that were imported plus the detection call we added.

        processedExpressionDataVectorService.computeProcessedExpressionData( ee );

        ee = eeService.thaw( ee );
        Collection<ProcessedExpressionDataVector> dataVectors = ee.getProcessedExpressionDataVectors();
        assertEquals( 10, dataVectors.size() );

        for ( ProcessedExpressionDataVector v : dataVectors ) {
            assertNotNull( v.getRankByMax() );
            assertNotNull( v.getRankByMean() );
        }

        ee = eeService.load( ee.getId() );
        assertNotNull( ee );
        ee = this.eeService.thawLite( ee );
        qts = eeService.getQuantitationTypes( ee );
        assertEquals( 18, qts.size() );
        Path f = dataFileService.writeOrLocateProcessedDataFile( ee, true, true ).orElse( null );
        assertNotNull( f );
        assertTrue( Files.exists( f ) );
        assertTrue( Files.size( f ) > 0 );
    }

    /*
     * For bug 2312 - qts getting dropped.
     */
    @Test
    public void testFetchAndLoadGSE18707() {
        setUpDatasetFromGeo( "GSE18707" );

        // Mouse430A_2.
        ee = eeService.findByShortName( "GSE18707" );
        aclTestUtils.checkEEAcls( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );

        assertEquals( 1, qts.size() );
        QuantitationType qt = qts.iterator().next();
        assertEquals( "Processed Affymetrix Rosetta intensity values", qt.getDescription() );

        processedExpressionDataVectorService.computeProcessedExpressionData( ee );
        ee = eeService.thaw( ee );
        Set<ProcessedExpressionDataVector> dataVectors = ee.getProcessedExpressionDataVectors();
        assertEquals( 100, ee.getNumberOfDataVectors().intValue() );
        assertEquals( 100, dataVectors.size() );

        ee = eeService.findByShortName( "GSE18707" );
        assertNotNull( ee );

        qts = eeService.getQuantitationTypes( ee );

        assertEquals( 2, qts.size() );

    }

    @Test
    public void testFetchAndLoadGSE5949() throws Exception {
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "GSE5949short" ) ) );
        setUpDatasetFromGeo( "GSE5949" );
        ee = this.eeService.thawLite( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        assertEquals( 1, qts.size() );
        Geeq geeq = geeqService.calculateScore( ee, GeeqService.ScoreMode.all );
        assertNotNull( geeq.getId() );
        ee = this.eeService.thawLite( ee );
        assertEquals( geeq, ee.getGeeq() );
        assertEquals( 2, ee.getAuditTrail().getEvents().size() );
        // creation, followed by a GeeqEvent
        assertEquals( AuditAction.CREATE, ee.getAuditTrail().getEvents().get( 0 ).getAction() );
        assertNull( ee.getAuditTrail().getEvents().get( 0 ).getEventType() );
        AuditEvent ev2 = ee.getAuditTrail().getEvents().get( 1 );
        assertEquals( AuditAction.UPDATE, ev2.getAction() );
        assertNotNull( ev2.getEventType() );
        assertEquals( GeeqEvent.class, ev2.getEventType().getClass() );
    }

    @Test
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "shortTest" ) ) );

        /*
         * HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
         * http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
         */
        setUpDatasetFromGeo( "GSE674" );
        assertNotNull( this.ee );
        ExpressionExperiment newee = eeService.thaw( this.ee );

        /*
         * Test for bug 468 (merging of subsets across GDS's)
         */
        ExperimentalFactor factor = newee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertEquals( 2, factor.getFactorValues().size() ); // otherwise get 4.

        Collection<RawExpressionDataVector> vectors = newee.getRawExpressionDataVectors();

        vectors = rawExpressionDataVectorService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );

        BulkExpressionDataMatrix<Double> matrix = builder.getPreferredData();

        assertNotNull( matrix );

        assertEquals( 31, matrix.rows() );

        assertEquals( 15, matrix.columns() );

        // GSM10363 = D1-U133B
        this.testMatrixValue( newee, matrix, "200000_s_at", "GSM10363", 5722.0 );

        // GSM10380 = C7-U133A
        this.testMatrixValue( newee, matrix, "1007_s_at", "GSM10380", 1272.0 );

    }

    @Test
    @Category(SlowTest.class)
    public void testLoadGSE30521ExonArray() {
        setUpDatasetFromGeo( "GSE30521" );
        ee = this.eeService.thawLite( ee );

        /*
         * Should load okay, but should not load the data.
         */
        try {
            processedExpressionDataVectorService.computeProcessedExpressionData( ee );
            fail( "Should not have any data vectors for exon arrays on first loading" );
        } catch ( Exception e ) {
            // OK
        }


        // Test the update mechanism
        ee.setPrimaryPublication( null );
        for ( BioAssay ba : ee.getBioAssays() ) {
            ba.setOriginalPlatform( null );
        }
        eeService.update( ee );

        geoService.updateFromGEO( "GSE30521" );
        ee = eeService.load( ee.getId() );
        assertNotNull( ee );
        ee = this.eeService.thawLite( ee );
        assertNotNull( ee );
        assertNotNull( ee.getPrimaryPublication() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertTrue( !ba.getSampleUsed().getCharacteristics().isEmpty() );
            for ( Characteristic c : ba.getSampleUsed().getCharacteristics() ) {
                assertNotNull( c.getCategory() );
            }
            assertNotNull( ba.getOriginalPlatform() );
        }

    }

    @Test
    @Category(SlowTest.class)
    public void testLoadGSE28383ExonArray() {
        setUpDatasetFromGeo( "GSE28383" );
        ee = this.eeService.thawLite( ee );

        /*
         * Should load okay, even though it has no data. See bug 3981.
         */
        try {
            processedExpressionDataVectorService.computeProcessedExpressionData( ee );
            fail( "Should not have any data vectors for exon arrays on first loading" );
        } catch ( Exception e ) {
            // OK
        }

    }

    @SuppressWarnings("unused")
    private void printMatrix( DoubleMatrix<Object, Object> matrix ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "probe" );
        for ( Object columnName : matrix.getColNames() ) {
            buf.append( "\t" ).append( columnName );
        }
        buf.append( "\n" );
        for ( Object rowName : matrix.getRowNames() ) {
            buf.append( rowName );
            double[] array = matrix.getRowByName( rowName );
            for ( double array_element : array ) {
                buf.append( "\t" ).append( array_element );
            }
            buf.append( "\n" );
        }
        log.debug( buf.toString() );
    }

    private void testMatrixValue( ExpressionExperiment exp, BulkExpressionDataMatrix<Double> matrix, String probeToTest,
            String sampleToTest, double expectedValue ) {

        CompositeSequence soughtDesignElement = null;
        BioAssay soughtBioAssay = null;
        Collection<RawExpressionDataVector> vectors = exp.getRawExpressionDataVectors();
        vectors = rawExpressionDataVectorService.thaw( vectors );
        for ( RawExpressionDataVector vector : vectors ) {
            CompositeSequence de = vector.getDesignElement();
            if ( de.getName().equals( probeToTest ) ) {
                soughtDesignElement = de;
            }

            BioAssayDimension bad = vector.getBioAssayDimension();
            for ( BioAssay ba : bad.getBioAssays() ) {
                if ( ba.getAccession().getAccession().equals( sampleToTest ) ) {
                    soughtBioAssay = ba;
                }
            }

        }
        if ( soughtDesignElement == null || soughtBioAssay == null )
            fail( "didn't find values for " + sampleToTest );

        Double actualValue = matrix.get( soughtDesignElement, soughtBioAssay );
        assertNotNull( "No value for " + soughtBioAssay, actualValue );
        assertEquals( expectedValue, actualValue, 0.00001 );

    }

    // leave this here for adding temporary tests
    //    @Test
    //    public void test() {
    //        ExpressionExperiment newee = fetchASeries( "GSE16035" );
    //        Collection<RawExpressionDataVector> vectors = newee.getRawExpressionDataVectors();
    //
    //        rawExpressionDataVectorService.thaw( vectors );
    //
    //        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
    //
    //        ExpressionDataMatrix<Double> matrix = builder.getPreferredData();
    //        System.err.println( matrix );
    //    }

}
