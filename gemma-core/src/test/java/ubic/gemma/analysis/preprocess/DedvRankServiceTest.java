/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import ubic.gemma.analysis.preprocess.DedvRankService.Method;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DedvRankServiceTest extends AbstractGeoServiceTest {

    GeoDatasetService geoService;

    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        init();
    }

    @SuppressWarnings("unchecked")
    public void testComputeDevRankForExpressionExperiment() throws Exception {
        endTransaction();
        DedvRankService serv = ( DedvRankService ) this.getBean( "dedvRankService" );

        ExpressionExperiment ee = null;
        String path = getTestFileBasePath();

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        ExpressionExperiment existing = eeService.findByShortName( "GSE2982" );
        if ( existing == null ) {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "gse2982Short" ) );
            Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GSE2982", false, true, false );
            ee = results.iterator().next();
        } else {
            ee = existing;
        }

        serv.computeDevRankForExpressionExperiment( ee, Method.MAX );

        eeService.thaw( ee );
        for ( DesignElementDataVector d : ee.getDesignElementDataVectors() ) {
            assertNotNull( d.getRank() );
        }
    }

    @Override
    protected void init() {
        geoService = ( GeoDatasetService ) this.getBean( "geoDatasetService" );

    }

}
