/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author joseph
 */
public class RawAndProcessedExpressionDataVectorServiceGeoTest extends AbstractGeoServiceTest {

    @Autowired
    protected GeoService geoService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedService;

    private ExpressionExperiment newee = null;

    @After
    public void tearDown() {
        try {
            if ( newee != null && newee.getId() != null ) {
                expressionExperimentService.remove( newee );
            }
        } catch ( Exception ignored ) {

        }

    }

    @Test
    @Category(SlowTest.class)
    public void testFindByQt() throws Exception {

        try {

            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse432Short" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE432", false, true, false );
            newee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            if ( e.getData() instanceof List ) {
                newee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                newee = ( ExpressionExperiment ) e.getData();
            }
        }

        newee.setShortName( RandomStringUtils.insecure().nextAlphabetic( 12 ) );
        expressionExperimentService.update( newee );

        newee = this.expressionExperimentService.thawLite( newee );

        QuantitationType qt = null;
        for ( QuantitationType q : newee.getQuantitationTypes() ) {
            if ( q.getIsPreferred() ) {
                qt = q;
                break;
            }
        }

        assertNotNull( "QT is null", qt );

        Collection<BulkExpressionDataVector> preferredVectors = rawAndProcessedService.find( qt );

        assertNotNull( preferredVectors );
        assertEquals( 40, preferredVectors.size() );
    }

}