/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.loader.expression;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * Switching of platforms that have no composite sequences.
 *
 * @author Paul
 */
public class ExpressionExperimentPlatformSwitchTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private ExpressionExperimentPlatformSwitchService experimentPlatformSwitchService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    /**
     * for bug 3451
     */
    @Test
    @Category({ GeoTest.class, SlowTest.class })
    public void testGSE36025() {
        assumeThatResourceIsAvailable( "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi" );

        // GSE36025
        //
        // This dataset contains a mixture of GPL9250 and GPL13112
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        Collection<?> results = geoService.fetchAndLoad( "GSE36025", false, false, false );
        ExpressionExperiment ee = ( ExpressionExperiment ) results.iterator().next();
        Collection<ArrayDesign> arrayDesignsUsed = experimentService.getArrayDesignsUsed( ee );
        assertEquals( 2, arrayDesignsUsed.size() );
        Map<BioAssay, ArrayDesign> originalPlatforms = new HashMap<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertNull( ba.getOriginalPlatform() );
            originalPlatforms.put( ba, ba.getArrayDesignUsed() );
        }

        // switch to GPL13112
        ArrayDesign arrayDesign = arrayDesignService.findByShortName( "GPL13112" );
        assertNotNull( arrayDesign );
        arrayDesign = arrayDesignService.thaw( arrayDesign );
        experimentPlatformSwitchService.switchExperimentToArrayDesign( ee, arrayDesign );

        arrayDesignsUsed = experimentService.getArrayDesignsUsed( ee );
        assertEquals( 1, arrayDesignsUsed.size() );
        assertEquals( arrayDesign, arrayDesignsUsed.iterator().next() );

        ee = experimentService.loadAndThaw( ee.getId() );
        assertNotNull( ee );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertNotNull( ba.getOriginalPlatform() );
            assertEquals( originalPlatforms.get( ba ), ba.getOriginalPlatform() );
            assertEquals( arrayDesign, ba.getArrayDesignUsed() );
        }
    }

    @Test
    public void testPlatformSwitchingWithExpressionData() {
        ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );

        assertEquals( 16, ee.getBioAssays().size() );
        assertEquals( 24, ee.getRawExpressionDataVectors().size() );

        Set<ArrayDesign> currentPlatforms = ee.getBioAssays().stream()
                .map( BioAssay::getArrayDesignUsed ).collect( Collectors.toSet() );
        // create a new platform, mapped one-to-one to the existing AD
        ArrayDesign newPlatform = new ArrayDesign();
        newPlatform.setTechnologyType( currentPlatforms.iterator().next().getTechnologyType() );
        newPlatform.setPrimaryTaxon( currentPlatforms.iterator().next().getPrimaryTaxon() );
        for ( ArrayDesign currentPlatform : currentPlatforms ) {
            for ( CompositeSequence probe : currentPlatform.getCompositeSequences() ) {
                CompositeSequence cs = new CompositeSequence();
                cs.setName( probe.getName() + "_remapped" );
                cs.setArrayDesign( newPlatform );
                cs.setBiologicalCharacteristic( probe.getBiologicalCharacteristic() );
                newPlatform.getCompositeSequences().add( cs );
            }
        }
        newPlatform = arrayDesignService.create( newPlatform );

        experimentPlatformSwitchService.switchExperimentToArrayDesign( ee, newPlatform );

        // reload from the database
        ee = experimentService.loadAndThaw( ee.getId() );
        assertNotNull( ee );
        assertEquals( 8, ee.getBioAssays().size() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( newPlatform, ba.getArrayDesignUsed() );
            assertNotNull( ba.getOriginalPlatform() );
            assertTrue( currentPlatforms.contains( ba.getOriginalPlatform() ) );
        }

        assertEquals( 24, ee.getRawExpressionDataVectors().size() );
        for ( RawExpressionDataVector vector : ee.getRawExpressionDataVectors() ) {
            assertEquals( 8, vector.getBioAssayDimension().getBioAssays().size() );
        }
    }

    @After
    public void tearDown() {
        ExpressionExperiment e1 = experimentService.findByShortName( "GSE36025" );
        if ( e1 != null ) {
            experimentService.remove( e1 );
        }
    }
}
