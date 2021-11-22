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
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    /**
     * for bug 3451
     */
    @Test
    @Category(SlowTest.class)
    public void test() {
        // GSE36025
        //
        // GPL9250
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        Collection<?> results = geoService.fetchAndLoad( "GSE36025", false, false, false );
        ExpressionExperiment ee = ( ExpressionExperiment ) results.iterator().next();
        results = geoService.fetchAndLoad( "GPL13112", true, false, false );
        ArrayDesign arrayDesign = ( ArrayDesign ) results.iterator().next();
        arrayDesign = arrayDesignService.thaw( arrayDesign );

        ee = experimentPlatformSwitchService.switchExperimentToArrayDesign( ee, arrayDesign );
        assertNotNull( ee );
        Collection<ArrayDesign> arrayDesignsUsed = experimentService.getArrayDesignsUsed( ee );

        assertEquals( 1, arrayDesignsUsed.size() );

        assertEquals( arrayDesign, arrayDesignsUsed.iterator().next() );
    }

    @After
    public void tearDown() {
        ExpressionExperiment e1 = experimentService.findByShortName( "GSE36025" );
        if ( e1 != null ) {
            experimentService.remove( e1 );
        }
    }
}
