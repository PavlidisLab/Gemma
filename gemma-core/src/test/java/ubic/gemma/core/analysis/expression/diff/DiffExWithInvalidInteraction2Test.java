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
package ubic.gemma.core.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * for bug 3927
 * 
 * @author Paul
 */
public class DiffExWithInvalidInteraction2Test extends AbstractGeoServiceTest {

    @Autowired
    private DifferentialExpressionAnalyzerService analyzer;

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
                .resourceToPath( "/data/analysis/expression" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE37301", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();

        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> toremove = new HashSet<>();
        toremove.addAll( ee.getExperimentalDesign().getExperimentalFactors() );
        for ( ExperimentalFactor ef : toremove ) {
            experimentalFactorService.delete( ef );
            ee.getExperimentalDesign().getExperimentalFactors().remove( ef );

        }

        expressionExperimentService.update( ee );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        ee = expressionExperimentService.thaw( ee );

        designImporter.importDesign( ee,
                this.getClass().getResourceAsStream( "/data/analysis/expression/7737_GSE37301_expdesign.data.txt" ) );

    }

    @After
    public void teardown() throws Exception {
        if ( ee != null ) expressionExperimentService.delete( ee );
    }

    @Test
    public void test() throws Exception {

        ee = expressionExperimentService.thawLite( ee );
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 3, factors.size() ); // includes batch

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( 3, ba.getSampleUsed().getFactorValues().size() );
        }

        ExperimentalFactor strain = null;
        ExperimentalFactor cell_type = null;
        for ( ExperimentalFactor ef : factors ) {
            if ( ef.getCategory().getValue().equals( "strain" ) ) {
                strain = ef;
            } else if ( ef.getCategory().getValue().equals( "cell type" ) ) {
                cell_type = ef;
            }
        }
        assertNotNull( cell_type );
        assertNotNull( strain );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.getFactorsToInclude().add( strain );
        config.getFactorsToInclude().add( cell_type );
        config.addInteractionToInclude( cell_type, strain );
        config.setQvalueThreshold( null );

        Collection<DifferentialExpressionAnalysis> result = analyzer.runDifferentialExpressionAnalyses( ee, config );
        assertEquals( 1, result.size() );
    }

}
