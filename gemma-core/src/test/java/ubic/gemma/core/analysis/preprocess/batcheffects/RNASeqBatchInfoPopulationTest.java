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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Settings;

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

        // bad ones. Should always get the platform

        String s4 = s.parseFASTQHeaderForBatch( "GPL1234;;;@SRR039864.1.1 VAB_KCl_hr0_total_RNA_b1_t11_48_981 length=35" ).toString();
        assertEquals( "Device=GPL1234", s4 );

        String s5 = s.parseFASTQHeaderForBatch( "GPL1234;;;@SRR5680873.1.1 1 length=101" ).toString();
        assertEquals( "Device=GPL1234", s5 );

    }

    @Before
    public void setup() throws Exception {
        Settings.setProperty( "gemma.fastq.headers.dir",
                new File( getClass().getResource( "/data/analysis/preprocess/batcheffects/fastqheaders" ).toURI() ).getAbsolutePath() );
    }

    /**
     * Test of creating batch factor. GSE71229 has two lanes
     * 
     * @throws Exception
     */
    @Test
    public void testGetBatches() throws Exception {

        GeoService geoService = this.getBean( GeoService.class );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );

        Collection<ExpressionExperiment> ees = eeService.findByAccession( "GSE71229" );
        if ( !ees.isEmpty() ) {
            ee = ees.iterator().next();
            eeService.remove( ee );
        }
        geoService.fetchAndLoad( "GSE71229", false, false, false );

        ees = eeService.findByAccession( "GSE71229" );
        ee = ees.iterator().next();
        ee = eeService.thawLite( ee );

        boolean success = batchInfoPopulationService.fillBatchInformation( ee, true );

        assertTrue( success );

        BatchEffectDetails batchEffect = eeService.getBatchEffect( ee );

        assertNotNull( batchEffect );

        ee = eeService.thawLite( ee );
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        ExperimentalFactor ef = experimentalFactors.iterator().next();
        assertEquals( "batch", ef.getName() );
        assertEquals( 2, ef.getFactorValues().size() );
    }

    @Test
    public void testBatchA() throws Exception {
        // GSE21161; nousable batch information
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE21161" );
        try {
            s.convertHeadersToBatches( h.values() );
            fail( "Should have gotten an exception" );
        } catch ( Exception expected ) {

        }
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

    @Test
    public void testBatchD() throws Exception {
        //GSE68376 - insufficient headers 
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE68376" );
        try {
            s.convertHeadersToBatches( h.values() );
            fail( "Should have gotten an exception" );
        } catch ( Exception expected ) {

        }
    }

    @Test
    public void testBatchDX() throws Exception {
        //GSE68376x - modified to have two platforms; headers are not usable.
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE68376x" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );
    }

    @Test
    public void testBatchE() throws Exception {
        //GSE70484 - has FAILUREs, so we should get no batches
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE70484" );

        try {
            s.convertHeadersToBatches( h.values() );
            fail( "Should have gotten an exception" );
        } catch ( Exception expected ) {

        }
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

    @Test
    public void testBatchG() throws Exception {
        //GSE77891; only has 4 samples. FIXME
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE77891" );
        Map<String, Collection<String>> batches = s.convertHeadersToBatches( h.values() );
        assertEquals( 2, batches.size() );
    }

    @Test
    public void testBatchH() throws Exception {
        //GSE78270  has two samples that don't have complete headers
        BatchInfoPopulationHelperServiceImpl s = new BatchInfoPopulationHelperServiceImpl();
        BatchInfoPopulationServiceImpl bs = new BatchInfoPopulationServiceImpl();
        Map<String, String> h = bs.readFastqHeaders( "GSE78270" );
        try {
            s.convertHeadersToBatches( h.values() );
            fail( "Should have gotten an exception" );
        } catch ( Exception expected ) {

        }
    }

    @After
    public void teardown() {
        if ( ee != null )
            eeService.remove( ee );
    }

}