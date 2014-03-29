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
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Tests loading, platform switch, vector merge, and complex deletion (in teardown)
 * 
 * @author paul
 * @version $Id$
 */
public class VectorMergingServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private ArrayDesignMergeService arrayDesignMergeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private ExpressionExperiment ee = null;

    @Autowired
    private ExpressionExperimentPlatformSwitchService eePlatformSwitchService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private GeoService geoService;

    private ArrayDesign mergedAA = null;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private VectorMergingService vectorMergingService;

    @After
    public void tearDown() {
        try {
            if ( ee != null ) eeService.delete( ee );
            if ( mergedAA != null ) {
                Collection<ArrayDesign> mergees = mergedAA.getMergees();
                mergedAA.setMergees( new HashSet<ArrayDesign>() );
                arrayDesignService.update( mergedAA );

                for ( ArrayDesign arrayDesign : mergees ) {
                    arrayDesign.setMergedInto( null );
                    arrayDesignService.update( arrayDesign );
                }
                arrayDesignService.remove( mergedAA );
                for ( ArrayDesign arrayDesign : mergees ) {
                    arrayDesignService.remove( arrayDesign );
                }
            }
        } catch ( Exception e ) {
            // oh well.
            log.info( e.getMessage(), e );
        }
    }

    @Test
    final public void test() throws Exception {
        tearDown();
        /*
         * Need a persistent experiment that uses multiple array designs. Then merge the designs, switch the vectors,
         * and merge the vectors. GSE3443
         */

        /*
         * The experiment uses the following GPLs
         * 
         * GPL2868, GPL2933, GPL2934, GPL2935, GPL2936, GPL2937, GPL2938
         * 
         * Example of a sequence appearing on more than one platform: N57553
         */
        ee = eeService.findByShortName( "GSE3443" );
        if ( ee != null ) {
            eeService.delete( ee ); // might work, but array designs might be in the way.
        }

        geoService
                .setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse3443merge" ) ) );

        Collection<?> results = geoService.fetchAndLoad( "GSE3443", false, false, true, false );
        ee = ( ExpressionExperiment ) results.iterator().next();

        ee = eeService.thawLite( ee );

        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );

        assertEquals( 7, aas.size() );

        /*
         * Check number of sequences across all platforms. This is how many elements we need on the new platform, plus
         * extras for duplicated sequences (e.g. elements that don't have a sequence...)
         */
        Collection<ArrayDesign> taas = new HashSet<ArrayDesign>();
        Set<BioSequence> oldbs = new HashSet<BioSequence>();
        for ( ArrayDesign arrayDesign : aas ) {
            ArrayDesign ta = arrayDesignService.thaw( arrayDesign );
            taas.add( ta );
            for ( CompositeSequence cs : ta.getCompositeSequences() ) {
                log.info( cs + " " + cs.getBiologicalCharacteristic() );
                oldbs.add( cs.getBiologicalCharacteristic() );
            }
        }
        assertEquals( 63, oldbs.size() );

        /*
         * Check total size of elements across all 7 platforms.
         */
        int totalElements = 0;
        for ( ArrayDesign arrayDesign : taas ) {
            totalElements += arrayDesign.getCompositeSequences().size();
        }
        assertEquals( 140, totalElements );

        ArrayDesign firstaa = taas.iterator().next();
        aas.remove( firstaa );

        mergedAA = arrayDesignMergeService.merge( firstaa, taas, "testMerge" + RandomStringUtils.randomAlphabetic( 5 ),
                "merged" + RandomStringUtils.randomAlphabetic( 5 ), false );

        assertEquals( 72, mergedAA.getCompositeSequences().size() );

        Set<BioSequence> seenBs = new HashSet<BioSequence>();
        for ( CompositeSequence cs : mergedAA.getCompositeSequences() ) {
            seenBs.add( cs.getBiologicalCharacteristic() );
        }
        assertEquals( 63, seenBs.size() );

        // just to make this explicit. The new array design has to contain all the old sequences.
        assertEquals( oldbs.size(), seenBs.size() );

        ee = eeService.thaw( ee );
        assertEquals( 1828, ee.getRawExpressionDataVectors().size() );

        ee = eePlatformSwitchService.switchExperimentToArrayDesign( ee, mergedAA );
        ee = eeService.thaw( ee );
        // check we actually got switched over.
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( mergedAA, ba.getArrayDesignUsed() );
        }
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            assertEquals( mergedAA, v.getDesignElement().getArrayDesign() );
        }

        assertEquals( 15, ee.getQuantitationTypes().size() );
        assertEquals( 1828, ee.getRawExpressionDataVectors().size() );

        ee = vectorMergingService.mergeVectors( ee );
        // ee = eeService.thaw( ee );

        // check we got the right processed data
        Collection<ProcessedExpressionDataVector> pvs = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );

        assertEquals( 72, pvs.size() );

        Collection<DoubleVectorValueObject> processedDataArrays = processedExpressionDataVectorService
                .getProcessedDataArrays( ee, 50 );

        assertEquals( 50, processedDataArrays.size() );

    }

}
