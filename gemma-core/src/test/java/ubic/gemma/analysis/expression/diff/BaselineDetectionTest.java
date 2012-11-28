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
package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.ontology.providers.MgedOntologyService;

/**
 * @author paul
 * @version $Id$
 */
public class BaselineDetectionTest extends AbstractGeoServiceTest {

    @Autowired
    protected GeoService geoService;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    ExperimentalDesignService experimentalDesignService;

    @Autowired
    ExperimentalFactorService experimentalFactorService;

    @Autowired
    OntologyService os;

    ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {
        MgedOntologyService mgedOntologyService = os.getMgedOntologyService();
        if ( !mgedOntologyService.isOntologyLoaded() ) {
            log.warn( "Need to load MGED!!!" );
            mgedOntologyService.startInitializationThread( true );
            while ( !mgedOntologyService.isOntologyLoaded() ) {
                Thread.sleep( 1000 );
                log.info( "Waiting for Ontology to load" );
            }
        }

        String path = FileTools.resourceToPath( "/data/loader/expression/geo/gse18162Short" );
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE18162", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            // OK.
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        }
        ee = eeService.thawLite( ee );
        if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            ee = eeService.load( ee.getId() );
            ee = eeService.thawLite( ee );

            InputStream is = this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/gse18162Short/design.txt" );
            experimentalDesignImporter.importDesign( ee, is, false );

            ee = eeService.load( ee.getId() );
            ee = eeService.thawLite( ee );
        }
        // end setup
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            try {
                eeService.delete( ee );
            } catch ( Exception e ) {

            }
        }
    }

    @Test
    public void testFetchAndLoadGSE18162() {

        Map<ExperimentalFactor, FactorValue> baselineLevels = ExpressionDataMatrixColumnSort.getBaselineLevels( ee
                .getExperimentalDesign().getExperimentalFactors() );

        assertEquals( 2, baselineLevels.size() ); // the batch DOES get a baseline. IF we change that then we change
                                                  // this test.
        for ( ExperimentalFactor ef : baselineLevels.keySet() ) {
            if ( ef.getName().equals( "batch" ) ) continue;
            FactorValue fv = baselineLevels.get( ef );
            assertEquals( "Control_group", fv.getValue() );
        }

    }
}
