/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.AbstractGeoServiceTest;

/**
 * @author paul
 * @version $Id$
 */
public class GeoSuperSeriesLoadIntegrationTest extends AbstractGeoServiceTest {
    protected AbstractGeoService geoService;

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadSuperSeries() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "GSE11897SuperSeriesShort" ) );
        Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                "GSE11897", false, true, false, false, true );
        assertEquals( 1, results.size() );

    }

    protected void onSetUp() throws Exception {
        super.onSetUp();
        init();
    }

    @Override
    protected void init() {
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
    }

}
