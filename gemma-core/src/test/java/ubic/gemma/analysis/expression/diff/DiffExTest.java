/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.analysis.expression.diff;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Tests added to check various cases of differential expression analysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class DiffExTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    ExperimentalDesignService experimentalDesignService;
    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;
    @Autowired
    ExperimentalFactorService experimentalFactorService;

    @Autowired
    private DiffExAnalyzer analyzer = null;

    /**
     * Test where probes have constant values. See bug 3177. This test really could be moved to a lower-level one in
     * bascode.
     */
    @Test
    public void testGSE35930() throws Exception {
        String path = getTestFileBasePath();
        ExpressionExperiment ee;
        // eeService.delete( eeService.findByShortName( "GSE35930" ) );
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "GSE35930" ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE35930", false, true, false, false );
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

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            ee = eeService.load( ee.getId() );
            ee = eeService.thawLite( ee );

            InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/geo/GSE35930/design.txt" );
            experimentalDesignImporter.importDesign( ee, is, false );

            ee = eeService.load( ee.getId() );
            ee = eeService.thawLite( ee );
        }

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee );
        assertNotNull( analyses );
        assertEquals( 1, analyses.size() );

        DifferentialExpressionAnalysis results = analyses.iterator().next();

        boolean found = false;
        ExpressionAnalysisResultSet resultSet = results.getResultSets().iterator().next();
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            Double pvalue = r.getPvalue();
            // this probe has a constant value
            if ( r.getProbe().getName().equals( "1622910_at" ) ) {
                found = true;
                assertTrue( "Got: " + pvalue, pvalue == null || pvalue.equals( Double.NaN ) );
            }
        }
        assertTrue( found );
    }

}
