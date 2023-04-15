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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.basecode.dataStructure.Link;
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

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private ExpressionExperimentPlatformSwitchService eePlatformSwitchService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private VectorMergingService vectorMergingService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private PreprocessorService preprocessorService;

    private ExpressionExperiment ee = null;
    private Collection<ArrayDesign> aas = null;
    private ArrayDesign mergedAA = null;

    @After
    public void tearDown() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
        LinkedHashSet<ArrayDesign> toRemove = new LinkedHashSet<>();
        if ( aas != null ) {
            Set<ArrayDesign> mergedInto = aas.stream().map( ArrayDesign::getMergedInto ).filter( Objects::nonNull ).collect( Collectors.toSet() );
            toRemove.addAll( aas );
            toRemove.addAll( mergedInto );
            arrayDesignService.remove( aas );
        }
        if ( mergedAA != null ) {
            toRemove.add( mergedAA );
            toRemove.addAll( mergedAA.getMergees() );
        }
        arrayDesignService.remove( toRemove );
    }

    @Test
    @Category(SlowTest.class)
    @Ignore("There's a regression that will be fixed in a subsequent patch release (see https://github.com/PavlidisLab/Gemma/issues/651)")
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

        aas = eeService.getArrayDesignsUsed( ee );
        aas = arrayDesignService.thaw( aas );
        assertEquals( 7, aas.size() );

        /*
         * Check number of sequences across all platforms. This is how many elements we need on the new platform, plus
         * extras for duplicated sequences (e.g. elements that don't have a sequence...)
         */
        Set<BioSequence> oldbs = new HashSet<>();
        for ( ArrayDesign arrayDesign : aas ) {
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                oldbs.add( cs.getBiologicalCharacteristic() );
            }
        }
        assertEquals( 63, oldbs.size() );

        /*
         * Check total size of elements across all 7 platforms.
         */
        int totalElements = 0;
        for ( ArrayDesign arrayDesign : aas ) {
            totalElements += arrayDesign.getCompositeSequences().size();
        }
        assertEquals( 140, totalElements );

        // make sure that the platforms are not already merged
        for ( ArrayDesign aa : aas ) {
            assertNull( aa.getMergedInto() );
            assertTrue( aa.getMergees().isEmpty() );
        }

        Iterator<ArrayDesign> it = aas.iterator();
        ArrayDesign firstAA = it.next();
        Collection<ArrayDesign> remainingAAs = new HashSet<>();
        it.forEachRemaining( remainingAAs::add );

        mergedAA = arrayDesignMergeService.merge( firstAA, remainingAAs, "testMerge" + RandomStringUtils.randomAlphabetic( 5 ),
                "merged" + RandomStringUtils.randomAlphabetic( 5 ), false );

        // ensure a new platform is created
        assertNotEquals( firstAA, mergedAA );
        assertEquals( 72, mergedAA.getCompositeSequences().size() );

        Set<BioSequence> seenBs = new HashSet<>();
        for ( CompositeSequence cs : mergedAA.getCompositeSequences() ) {
            seenBs.add( cs.getBiologicalCharacteristic() );
        }
        assertEquals( 63, seenBs.size() );

        // just to make this explicit. The new array design has to contain all the old sequences.
        assertEquals( oldbs.size(), seenBs.size() );

        ee = eeService.thaw( ee );

        ee = eePlatformSwitchService.switchExperimentToArrayDesign( ee, mergedAA );
        ee = eeService.thaw( ee );
        // check we actually got switched over.
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( mergedAA, ba.getArrayDesignUsed() );
        }
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            assertEquals( mergedAA, v.getDesignElement().getArrayDesign() );
        }

        assertEquals( 16, ee.getQuantitationTypes().size() );
        assertEquals( 1828, ee.getRawExpressionDataVectors().size() );

        vectorMergingService.mergeVectors( ee );

        // make sure the EE is up-to-date
        ee = eeService.thaw( ee );

        assertEquals( 46, ee.getNumberOfSamples().intValue() );
        assertEquals( 978, ee.getRawExpressionDataVectors().size() );
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
                .getProcessedDataArrays( ee, 50 );

        assertEquals( 50, processedDataArrays.size() );

    }

}
