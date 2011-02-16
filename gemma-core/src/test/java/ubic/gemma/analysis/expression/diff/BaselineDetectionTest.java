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
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.ConfigUtils;

/**
 * @author paul
 * @version $Id$
 */
public class BaselineDetectionTest extends AbstractGeoServiceTest {

    @Autowired
    protected GeoDatasetService geoService;

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

    @Test
    public void testFetchAndLoadGSE18162() throws Exception {
        // setup
        os.getMgedOntologyService().startInitializationThread( true );
        while ( !os.getMgedOntologyService().isOntologyLoaded() ) {
            Thread.sleep( 1000 );
            log.info( "Waiting for Ontology to load" );
        }

        String path = ConfigUtils.getString( "gemma.home" );
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE18162", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            // OK.
            ee = ( ExpressionExperiment ) e.getData();
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

        Map<ExperimentalFactor, FactorValue> baselineLevels = ExpressionDataMatrixColumnSort.getBaselineLevels( ee
                .getExperimentalDesign().getExperimentalFactors() );

        assertEquals( 1, baselineLevels.size() ); // the batch does not get a baseline.
        for ( ExperimentalFactor ef : baselineLevels.keySet() ) {
            FactorValue fv = baselineLevels.get( ef );
            assertEquals( "Control_group", fv.getValue() );
        }

        // eeService.delete( ee );

        // assertNull( eeService.load( ee.getId() ) );

    }
}
