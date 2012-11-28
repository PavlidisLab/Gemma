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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.preprocess.VectorMergingService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class GeoSuperSeriesLoadIntegrationTest extends AbstractGeoServiceTest {

    @Autowired
    protected GeoService geoService;

    @Autowired
    ArrayDesignMergeService adms;

    @Autowired
    VectorMergingService vms;

    @Autowired
    ExpressionExperimentPlatformSwitchService eepss;

    @Autowired
    ExpressionExperimentService ees;

    ExpressionExperiment ee;

    @After
    public void tearDown() {
        if ( ee != null ) {
            try {
                ees.delete( ee );
            } catch ( Exception e ) {

            }
        }
    }

    @Test
    public void testFetchAndLoadSuperSeries() throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                getTestFileBasePath( "GSE11897SuperSeriesShort" ) ) );
        try {
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE11897", false, true, false, false, true, false );
            assertEquals( 1, results.size() );
            ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) e.getData();
        }

    }

    /**
     * See bug 2064. GSE14618 is a superseries of GSE14613 and GSE14615
     * 
     * @throws Exception
     */
    @Test
    public void testFetchAndLoadSuperSeriesB() throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                getTestFileBasePath( "gse14618superser" ) ) );

        Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                "GSE14618", false, true, false, false, true, false );
        assertEquals( 1, results.size() );

        ee = results.iterator().next();

        ee = ees.findByShortName( "GSE14618" );
        ee = ees.thawLite( ee );
        Collection<QuantitationType> qts = ee.getQuantitationTypes();

        assertEquals( 1, qts.size() );

        Collection<ArrayDesign> arrayDesignsUsed = ees.getArrayDesignsUsed( ee );

        Collection<ArrayDesign> others = new HashSet<ArrayDesign>();
        others.add( ( ArrayDesign ) arrayDesignsUsed.toArray()[1] );

        ArrayDesign arrayDesign = ( ArrayDesign ) arrayDesignsUsed.toArray()[0];
        ArrayDesign merged = adms.merge( arrayDesign, others,
                RandomStringUtils.randomAlphabetic( 5 ), RandomStringUtils.randomAlphabetic( 5 ), false );

        eepss.switchExperimentToArrayDesign( ee, merged );

        vms.mergeVectors( ee );

        ee = ees.load( ee.getId() );

        ee = ees.findByShortName( "GSE14618" );
        ee = ees.thaw( ee );

        assertEquals( 40, ee.getProcessedExpressionDataVectors().size() );
        // System.err.println( ee.getProcessedExpressionDataVectors().size() );
        boolean found1 = false;
        boolean found2 = false;

        ByteArrayConverter bac = new ByteArrayConverter();
        for ( DesignElementDataVector v : ee.getProcessedExpressionDataVectors() ) {
            double[] dat = bac.byteArrayToDoubles( v.getData() );
            int count = 0;

            assertEquals( 92, dat.length );
            if ( v.getDesignElement().getName().equals( "117_at" ) ) {
                found1 = true;
                for ( double d : dat ) {
                    if ( Double.isNaN( d ) ) {
                        count++;
                    }
                }
                assertEquals( "Should have been no missing values", 0, count );
            } else if ( v.getDesignElement().getName().equals( "1552279_a_at" ) ) {
                found2 = true;
                for ( double d : dat ) {
                    if ( Double.isNaN( d ) ) {
                        count++;
                    }
                }
                assertEquals( "Wrong number of missing values", 42, count );
            }
        }

        assertTrue( "Didn't find first test probe expected.", found1 );
        assertTrue( "Didn't find second test probe expected.", found2 );

    }
}
