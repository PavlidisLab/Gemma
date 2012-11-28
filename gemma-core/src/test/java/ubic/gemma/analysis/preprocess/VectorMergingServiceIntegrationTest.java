/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Need an empty db.
 * 
 * @author paul
 * @version $Id$
 */
public class VectorMergingServiceIntegrationTest extends AbstractGeoServiceTest {

    @Autowired
    VectorMergingService vectorMergingService;

    @Autowired
    ArrayDesignMergeService arrayDesignMergeService;

    @Autowired
    ExpressionExperimentPlatformSwitchService eePlatformSwitchService;

    @Autowired
    GeoService geoService;

    @Autowired
    ExpressionExperimentService eeService;

    ExpressionExperiment ee;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    Collection<ArrayDesign> aas;
    ArrayDesign mergedAA;

    @Test
    final public void test() throws Exception {

        /*
         * Need a persistent experiment that uses multiple array designs. Then merge the designs, switch the vectors,
         * and merge the vectors. GSE3443
         */

        ee = eeService.findByShortName( "GSE3443" );

        if ( ee == null ) {

            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                    getTestFileBasePath( "gse3443merge" ) ) );

            Collection<?> results = geoService.fetchAndLoad( "GSE3443", false, false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        }

        ee = eeService.thawLite( ee );

        aas = eeService.getArrayDesignsUsed( ee );

        Collection<ArrayDesign> taas = new HashSet<ArrayDesign>();
        for ( ArrayDesign arrayDesign : aas ) {
            taas.add( arrayDesignService.thawLite( arrayDesign ) );
        }

        ArrayDesign firstaa = taas.iterator().next();
        aas.remove( firstaa );

        mergedAA = arrayDesignMergeService.merge( firstaa, taas, "testMerge" + RandomStringUtils.randomAlphabetic( 5 ),
                "merged" + RandomStringUtils.randomAlphabetic( 5 ), false );

        eePlatformSwitchService.switchExperimentToArrayDesign( ee, mergedAA );

        vectorMergingService.mergeVectors( ee );

        Collection<DoubleVectorValueObject> processedDataArrays = processedExpressionDataVectorService
                .getProcessedDataArrays( ee, 50 );

        // System.err.println( processedDataArrays.size() );

        assertEquals( 50, processedDataArrays.size() );

        /*
         * check results
         */
        // for ( DoubleVectorValueObject v : processedDataArrays ) {
        // System.err.println( StringUtils.join( v.getData(), '\t' ) );
        // }

    }

    @After
    public void tearDown() {
        // fails, due to constraints.
        // try {
        // eeService.delete( ee );
        // mergedAA.getMergees().clear();
        // arrayDesignService.update( mergedAA );
        //
        // for ( ArrayDesign arrayDesign : aas ) {
        // arrayDesign.setMergedInto( null );
        // arrayDesignService.update( arrayDesign );
        // }
        // arrayDesignService.remove( mergedAA );
        // for ( ArrayDesign arrayDesign : aas ) {
        // arrayDesignService.remove( arrayDesign );
        // }
        // } catch ( Exception e ) {
        // // oh well.
        // log.info( e.getMessage() );
        // }
    }

}
