/*
 * The gemma-core project
 *
 * Copyright (c) 2019 University of British Columbia
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

package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SingleBatchDeterminationEvent;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Settings;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 * @author paul
 */
public class RNASeqBatchInfoPopulationTest extends AbstractGeoServiceTest {

    @Autowired
    private BatchInfoPopulationService batchInfoPopulationService;

    @Autowired
    private ExpressionExperimentService eeService;

    private ExpressionExperiment ee;

    @Autowired
    private AuditEventService auditService;

    @Autowired
    private GeoService geoService;

    @Test
    public void testParseHeaders() {
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();

        // 5-field version
        String s1 = s.parseFASTQHeaderForBatch( "GPL1234;;;@SRR2016978.1.2 EAS042_0001:2:1:1044:11321 length=76" ).toString();
        assertEquals( "Device=EAS042_0001:Lane=2", s1 );

        // multiple header case
        String s2 = s.parseFASTQHeaderForBatch(
                "GPL1234;;;@SRR2016978.1.1 EAS042_0001:2:1:1044:11321 length=76;;;@SRR2016978.1.2 EAS042_0001:2:1:1044:11321 length=76" ).toString();
        assertEquals( "Device=EAS042_0001:Lane=2", s2 );

        // 7-field version
        String s3 = s.parseFASTQHeaderForBatch( "GPL1234;;;@SRR5647782.1.1 D7ZQJ5M1:747:HL5TJADXX:1:1116:18513:98450 length=101" ).toString();
        assertEquals( "Device=D7ZQJ5M1:Run=747:Flowcell=HL5TJADXX:Lane=1", s3 );

        // bad ones. Should always get the platform but we keep the header as well as an extra fallback

        String s4 = s.parseFASTQHeaderForBatch( "GPL1234;;;@SRR039864.1.1 VAB_KCl_hr0_total_RNA_b1_t11_48_981 length=35" ).toString();
        assertEquals( "Device=GPL1234", s4 );

        String s5 = s.parseFASTQHeaderForBatch( "GPL1234;;;@SRR5680873.1.1 1 length=101" ).toString();
        assertEquals( "Device=GPL1234", s5 );

    }

    @Before
    public void setUp() throws Exception {
        Settings.setProperty( "gemma.fastq.headers.dir",
                new ClassPathResource( "/data/analysis/preprocess/batcheffects/fastqheaders" ).getFile().getAbsolutePath() );
    }

    /**
     * Test of creating batch factor. GSE71229 has two lanes
     *
     */
    @Test
    @Category(SlowTest.class)
    public void testGetBatches() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );

        Collection<ExpressionExperiment> ees = eeService.findByAccession( "GSE71229" );
        if ( !ees.isEmpty() ) {
            ee = ees.iterator().next();
            eeService.remove( ee );
        }
        geoService.fetchAndLoad( "GSE71229", false, false, false );

        ees = eeService.findByAccession( "GSE71229" );
        ee = ees.iterator().next();

        batchInfoPopulationService.fillBatchInformation( ee, true );

        BatchEffectDetails batchEffect = eeService.getBatchEffectDetails( ee );

        assertNotNull( batchEffect );

        ee = eeService.thawLite( ee );
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        ExperimentalFactor ef = experimentalFactors.iterator().next();
        assertEquals( "batch", ef.getName() );
        assertEquals( 2, ef.getFactorValues().size() );

        assertTrue( auditService.hasEvent( ee, BatchInformationFetchingEvent.class ) );

        // now repeat it to make sure we don't end up with duplicate factors
        batchInfoPopulationService.fillBatchInformation( ee, true );
        ee = eeService.thawLite( ee );
        experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertEquals( 1, experimentalFactors.size() );
    }

    /*
     * See https://github.com/PavlidisLab/Gemma/issues/129 - should be a single batch because samples were run in one
     * lane
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE14285OneBatch() throws Exception {
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess/batcheffects/" ) ) );
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE142485", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE142485 was not removed from the system prior to test" );
        }
        batchInfoPopulationService.fillBatchInformation( ee, false );
        ee = eeService.thawLite( ee );
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertTrue( experimentalFactors.isEmpty() );

        assertTrue( auditService.hasEvent( ee, SingleBatchDeterminationEvent.class ) );
        assertTrue( this.eeService.checkHasBatchInfo( ee ) );
    }

    /*
     * See https://github.com/PavlidisLab/Gemma/issues/129. For this case, no headers are usable so we should get no
     * batch info.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE156689NoBatchinfo() throws Exception {
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess/batcheffects/" ) ) );
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE156689", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE156689 was not removed from the system prior to test" );
        }

        batchInfoPopulationService.fillBatchInformation( ee, false );

        ee = eeService.thawLite( ee );
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertTrue( experimentalFactors.isEmpty() );
        assertTrue( auditService.hasEvent( ee, FailedBatchInformationFetchingEvent.class ) );
        assertFalse( this.eeService.checkHasBatchInfo( ee ) );

    }

    @Test(expected = FASTQHeadersPresentButNotUsableException.class)
    public void testBatchA() throws Exception {
        // GSE21161; no good headers, we can't form batches 
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE21161" );

        s.convertHeadersToBatches( h.values() );

    }

    @Test
    public void testBatchB() throws Exception {
        //GSE33527 - has only one batch, everything done in one lane.
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE33527" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 1, batches.size() );
    }

    @Test
    public void testBatchC() throws Exception {
        //GSE66715 - easy case, has two batches by device
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE66715" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );
    }

    @Test(expected = FASTQHeadersPresentButNotUsableException.class)
    public void testBatchD() throws Exception {
        //GSE68376 - insufficient headers, all identical, one platform - probably should mark it as "no batch information"
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE68376" );

        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 0, batches.size() );
    }

    @Test
    public void testBatchDX() throws Exception {
        //GSE68376x - modified to have two platforms; headers are not usable, but salvageable by platform
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE68376x" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );
    }

    /**
     * See https://github.com/PavlidisLab/GemmaCuration/issues/64
     *
     */
    @Test
    public void testBatchMixedHeaders() throws Exception {
        //GSE153549 - has a mix of usable headers and not, so we should fall back on the platform for the ones that are not usable. 
        // For the usable headers, there are four lanes. For the unusable headers we just consider them as one batch.
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE153549" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 5, batches.size() );

        // second test but this time faking the platform all the same, but with two styles of headers. 
        // We should still find  5 batches here
        h = bs.readFastqHeaders( "GSE153549X" );
        batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 5, batches.size() );

        h = bs.readFastqHeaders( "GSE163323" );
        batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );

    }

    @Test(expected = SingletonBatchesException.class)
    public void testBatchMixedHeadersSinglePlatformSingleton() throws Exception {
        // only one sample has a usable header. There are two lines in the header file for this sample.
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE160025" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );

        //        for ( String b : batches.keySet() ) {
        //            log.info( "Batch: " + b );
        //            for ( String batchmember : batches.get( b ) ) {
        //                log.info( "   " + batchmember );
        //            }
        //        }
        // we don't assign batches when this happens
        assertEquals( 0, batches.size() );

    }

    @Test
    public void testBatchMixedHeadersSinglePlatform() throws Exception {
        // GSE157825 - has a mix of usable headers and not, and on just one platform; should lead to two batches
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE157825" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );

        assertEquals( 2, batches.size() );
    }

    @Test(expected = SingletonBatchesException.class)
    public void testBatchE() throws Exception {
        //GSE70484 - has FAILUREs, so we should get no batches (but has singletons as well)
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE70484" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 0, batches.size() );
    }

    @Test
    public void testBatchF() throws Exception {
        //GSE71229; batching by lane
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE71229" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );
    }

    @Test(expected = SingletonBatchesException.class)
    public void testBatchG() throws Exception {
        //GSE77891; only has 4 samples. There are two devices, but one of the batches is a singleton
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE77891" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 0, batches.size() );
    }

    @Test
    public void testBatchH() throws Exception {
        //GSE78270  has two samples that don't have complete headers, but we should still get batches
        // Since most of the headers are usable, and fall into 8 batches, the leftovers should be one batch
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE78270" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 9, batches.size() );
    }

    @Test
    public void testBatchI() throws Exception {
        //GSE73508 
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE73508" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 8, batches.size() );
    }

    @Test
    public void testBatchJ() throws Exception {
        //GSE59765
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE59765" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 4, batches.size() );
    }

    @Test
    public void testBatchK() throws Exception {
        //GSE55790 - has some underscore formatted, some not
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE55790" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 7, batches.size() );
    }

    @Test(expected = FASTQHeadersPresentButNotUsableException.class)
    public void testBatchL() throws Exception {
        //GSE51827 - has underscore format, three fields, considered unusable (ABI)
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE51827" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 0, batches.size() );
    }

    @Test(expected = SingletonBatchesException.class)
    public void testBatchM() throws Exception {
        //GSE62826 - was yielding an npe, has singletons, should yield no batches
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE62826" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 0, batches.size() );
    }

    @Test
    public void testBatchO() throws Exception {
        //GSE73508 - 
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE73508" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 8, batches.size() );
    }

    @Test(expected = SingletonBatchesException.class)
    public void testBatchP() throws Exception {
        //GSE111979 - this has a nine-field header, we don't handle this correctly but it's not supported anyway (single-cell)
        // [GTTCCCGT, @NS500264, 224, HWJ37BGXY, 1, 11101, 2193, 3352, TGCGTAAGCTTAGCCATCGCATTGCTATTTCTACCTCTGAGCTGAAACCCAAACGGTTCCCGTGACTT]
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE111979" );
        s.convertHeadersToBatches( h.values() );
    }

    @Test
    public void testBatchQ() throws Exception {
        //GSE173137 - 
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE173137" );
        s.convertHeadersToBatches( h.values() );
    }

    @Test
    public void testBatchR() throws Exception {
        // GSE83115 - singleton batches if we use lane, dropping to device gives two batches.
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE83115" );

        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );
    }

    @After
    public void teardown() {
        if ( ee != null )
            eeService.remove( ee );
    }

}