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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
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
        String s1 = s.parseFASTQHeaderForBatch( "@SRR2016978.1.2 EAS042_0001:2:1:1044:11321 length=76" );
        assertEquals( "Dev=EAS042_0001:Lane=2", s1 );

        // multiple header case
        String s2 = s.parseFASTQHeaderForBatch(
                "@SRR2016978.1.1 EAS042_0001:2:1:1044:11321 length=76\n@SRR2016978.1.2 EAS042_0001:2:1:1044:11321 length=76" );
        assertEquals( "Dev=EAS042_0001:Lane=2", s2 );

        // 7-field version
        String s3 = s.parseFASTQHeaderForBatch( "@SRR5647782.1.1 D7ZQJ5M1:747:HL5TJADXX:1:1116:18513:98450 length=101" );
        assertEquals( "Dev=D7ZQJ5M1:Run=747:Cell=HL5TJADXX:Lane=1", s3 );

        // bad ones.
        try {
            s.parseFASTQHeaderForBatch( "@SRR039864.1.1 VAB_KCl_hr0_total_RNA_b1_t11_48_981 length=35" );
            fail( "Should have gotten an exception" );
        } catch ( Exception expected ) {
            // ok
        }

        try {
            s.parseFASTQHeaderForBatch( "@SRR5680873.1.1 1 length=101" );
            fail( "Should have gotten an exception" );
        } catch ( Exception expected ) {
            // ok
        }
    }

    @Test
    public void testGetBatches() throws Exception {
        Settings.setProperty( "gemma.fastq.headers.dir",
                new File( this.getClass().getResource( "/data/analysis/preprocess/batcheffects/fastqheaders" ).toURI() ).getAbsolutePath() );

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

    }

    @After
    public void teardown() {
        if ( ee != null )
            eeService.remove( ee );
    }

}