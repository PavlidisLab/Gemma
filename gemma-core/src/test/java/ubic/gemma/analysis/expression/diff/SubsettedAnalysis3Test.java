/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
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

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class SubsettedAnalysis3Test extends AbstractGeoServiceTest {
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DiffExAnalyzer analyzer;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    private ExpressionExperiment ee;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private GeoService geoService;

    @Before
    public void setup() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( FileTools
                .resourceToPath( "/data/analysis/expression/gse26927short" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE26927", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();

        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> toremove = new HashSet<ExperimentalFactor>();
        toremove.addAll( ee.getExperimentalDesign().getExperimentalFactors() );
        for ( ExperimentalFactor ef : toremove ) {
            experimentalFactorService.delete( ef );
            ee.getExperimentalDesign().getExperimentalFactors().remove( ef );

        }

        expressionExperimentService.update( ee );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        ee = expressionExperimentService.thaw( ee );

        designImporter.importDesign(
                ee,
                this.getClass().getResourceAsStream(
                        "/data/analysis/expression/gse26927short/2684_GSE26927_expdesign.data.txt" ) );

    }

    @After
    public void teardown() throws Exception {
        // if ( ee != null ) expressionExperimentService.delete( ee );
    }

    @Test
    public void test() throws Exception {

        ee = expressionExperimentService.thawLite( ee );
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 3, factors.size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( 3, ba.getSampleUsed().getFactorValues().size() );
        }

        ExperimentalFactor organismpart = null;
        ExperimentalFactor disease = null;
        ExperimentalFactor diseasegroup = null;
        for ( ExperimentalFactor ef : factors ) {
            if ( ef.getCategory().getValue().equals( "study design" ) ) {
                diseasegroup = ef;
            } else if ( ef.getCategory().getValue().equals( "disease" ) ) {
                disease = ef;
            } else if ( ef.getCategory().getValue().equals( "organism part" ) ) {
                organismpart = ef;
            }
        }
        assertNotNull( diseasegroup );
        assertNotNull( disease );
        assertNotNull( organismpart );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.getFactorsToInclude().add( disease );
        config.getFactorsToInclude().add( organismpart );

        config.setSubsetFactor( diseasegroup );
        config.setQvalueThreshold( null );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, config );
        assertEquals( 6, analyses.size() ); // a subset for each disease: SZ, PD, HD, ALS, ALZ, MS

        /*
         * Now, within each we should have only one disease contrast,
         */
        for ( DifferentialExpressionAnalysis analysis : analyses ) {

            // there should be one for disease - tissue isn't used.
            assertEquals( 1, analysis.getResultSets().size() );

            for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
                ExperimentalFactor factor = rs.getExperimentalFactors().iterator().next();
                for ( DifferentialExpressionAnalysisResult res : rs.getResults() ) {
                    Collection<ContrastResult> contrasts = res.getContrasts();

                    for ( ContrastResult cr : contrasts ) {
                        log.info( analysis + "   " + factor + " " + cr + " " + res );
                    }
                    break;
                }
            }

        }

    }
}
