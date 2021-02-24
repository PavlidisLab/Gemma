/*
 * The gemma-core project
 * 
 * Copyright (c) 2021 University of British Columbia
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

import java.io.File;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;
import ubic.gemma.persistence.util.Settings;

/**
 * Tests for when an RNA-seq experiment has only one batch, or is missing batch information.
 * 
 * @author paul
 */
public class BatchEffectRNASeqExtractionTest extends AbstractGeoServiceTest {

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
    private ExpressionExperiment ee;
    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    @Autowired
    private ProcessedExpressionDataVectorService dataVectorService;
    @Autowired
    private GeeqService geeqService;
    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private BatchInfoPopulationService batchInfoPopulationService;

    // from RNASeqBatchInfoPopulationTest
    @Before
    public void setup() throws Exception {
        Settings.setProperty( "gemma.fastq.headers.dir",
                new File( getClass().getResource( "/data/analysis/preprocess/batcheffects/fastqheaders" ).toURI() ).getAbsolutePath() );
    }

    @Test
    public void testGSE14285OneBatch() throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( "/data/analysis/preprocess/batcheffects/" ) );
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE14285", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE14285 was not removed from the system prior to test" );
        }
        ee = eeService.thawLite( ee );
        batchInfoPopulationService.fillBatchInformation( ee, false );

    }

    @Test
    public void testGSE156689NoBatchinfo() throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( "/data/analysis/preprocess/batcheffects/" ) );
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE156689", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE156689 was not removed from the system prior to test" );
        }
    }

    @After
    public void tearDown() {
        if ( ee != null )
            try {
                eeService.remove( ee );
            } catch ( Exception e ) {
                log.info( "Failed to remove EE after test: " + e.getMessage() );
                throw e;
            }
    }

}
