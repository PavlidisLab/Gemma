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
package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests loading, platform switch, vector merge, and complex deletion (in teardown)
 *
 * @author paul
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

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Before
    @After
    public void tearDown() {
        try {
            ee = eeService.findByShortName( "GSE3443" );

            if ( ee != null ) {
                mergedAA = eeService.getArrayDesignsUsed( ee ).iterator().next();
                eeService.remove( ee );

                if ( mergedAA != null ) {
                    mergedAA.setMergees( new HashSet<ArrayDesign>() );
                    arrayDesignService.update( mergedAA );

                    mergedAA = arrayDesignService.thawLite( mergedAA );
                    for ( ArrayDesign arrayDesign : mergedAA.getMergees() ) {
                        arrayDesign.setMergedInto( null );
                        arrayDesignService.update( arrayDesign );
                    }

                    for ( ExpressionExperiment e : arrayDesignService.getExpressionExperiments( mergedAA ) ) {
                        eeService.remove( e );
                    }

                    arrayDesignService.remove( mergedAA );
                    for ( ArrayDesign arrayDesign : mergedAA.getMergees() ) {
                        for ( ExpressionExperiment e : arrayDesignService.getExpressionExperiments( arrayDesign ) ) {
                            eeService.remove( e );
                        }
                        arrayDesignService.remove( arrayDesign );

                    }
                }

            }
        } catch ( Exception e ) {
            log.info( "Tear-down failed: " + e.getMessage() );
        }
    }

    @Test
    @Category(SlowTest.class)
    final public void test() throws Exception {
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

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse3443merge" ) ) );

        Collection<?> results = geoService.fetchAndLoad( "GSE3443", false, false, false );
        ee = ( ExpressionExperiment ) results.iterator().next();

        ee = this.eeService.thawLite( ee );

        // fix for unknown log scale
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getIsPreferred() ) {
                qt.setScale( ScaleType.LOG2 );
                quantitationTypeService.update( qt );
            }
        }

        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );

        assertEquals( 7, aas.size() );

        /*
         * Check number of sequences across all platforms. This is how many elements we need on the new platform, plus
         * extras for duplicated sequences (e.g. elements that don't have a sequence...)
         */
        Collection<ArrayDesign> taas = new HashSet<>();
        Set<BioSequence> oldbs = new HashSet<>();
        for ( ArrayDesign arrayDesign : aas ) {
            arrayDesign = arrayDesignService.thaw( arrayDesign );
            taas.add( arrayDesign );
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
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

        assertNull( firstaa.getMergedInto() );

        mergedAA = arrayDesignMergeService.merge( firstaa, taas, "testMerge" + RandomStringUtils.randomAlphabetic( 5 ),
                "merged" + RandomStringUtils.randomAlphabetic( 5 ), false );

        assertEquals( 72, mergedAA.getCompositeSequences().size() );

        Set<BioSequence> seenBs = new HashSet<>();
        for ( CompositeSequence cs : mergedAA.getCompositeSequences() ) {
            seenBs.add( cs.getBiologicalCharacteristic() );
        }
        assertEquals( 63, seenBs.size() );

        // just to make this explicit. The new array design has to contain all the old sequences.
        assertEquals( oldbs.size(), seenBs.size() );

        ee = eeService.thaw( ee );

        eePlatformSwitchService.switchExperimentToArrayDesign( ee, mergedAA ); // prerequisite for vector merging
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

        vectorMergingService.mergeVectors( ee );

        // make sure the EE is up-to-date
        ee = eeService.thaw( ee );

        assertEquals( 46, ee.getNumberOfSamples().intValue() );
        assertEquals( 978, ee.getRawExpressionDataVectors().size() );
        assertEquals( 15, ee.getQuantitationTypes().size() );
        assertTrue( ee.getProcessedExpressionDataVectors().isEmpty() );
        assertEquals( 0, ee.getNumberOfDataVectors().intValue() );

        preprocessorService.process( ee, true, true );

        // make sure the EE is up-to-date
        ee = eeService.thaw( ee );

        // check we got the right processed data
        Collection<ProcessedExpressionDataVector> pvs = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );

        assertEquals( 72, pvs.size() );
        assertEquals( 72, ee.getNumberOfDataVectors().intValue() );
        // missing values are filled in, so we get more raw vectors
        assertEquals( 1033, ee.getRawExpressionDataVectors().size() );
        assertEquals( 46, ee.getNumberOfSamples().intValue() );

        Collection<DoubleVectorValueObject> processedDataArrays = processedExpressionDataVectorService
                .getRandomProcessedDataArrays( ee, 50 );

        assertEquals( 28, processedDataArrays.size() );

    }

}
