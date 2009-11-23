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

package ubic.gemma.model.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 * @version $Id$
 */
public class DifferentialExpressionAnalysisServiceTest extends BaseSpringContextTest {

    @Autowired
    private DifferentialExpressionAnalysisService analysisService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    // Test Data
    DifferentialExpressionAnalysis eAnalysis1;
    DifferentialExpressionAnalysis eAnalysis2;
    DifferentialExpressionAnalysis eAnalysis3;
    DifferentialExpressionAnalysis eAnalysis4;

    ExpressionExperiment e1;
    ExpressionExperiment e2;
    ExpressionExperiment e3;

    private String testEESetName;

    private String testAnalysisName;

    @Before
    public void setup() throws Exception {

        e1 = ExpressionExperiment.Factory.newInstance();
        e1.setShortName( RandomStringUtils.randomAlphabetic( 6 ) );
        e1 = expressionExperimentService.create( e1 );

        e2 = ExpressionExperiment.Factory.newInstance();
        e2.setShortName( RandomStringUtils.randomAlphabetic( 6 ) );
        e2 = expressionExperimentService.create( e2 );

        e3 = ExpressionExperiment.Factory.newInstance();
        e3.setShortName( RandomStringUtils.randomAlphabetic( 6 ) );
        e3 = expressionExperimentService.create( e3 );

        ExpressionExperimentSet eeSet = initSet();
        eeSet.getExperiments().add( e1 );
        expressionExperimentSetService.update( eeSet );

        // //////////////////
        eAnalysis1 = DifferentialExpressionAnalysis.Factory.newInstance();
        eAnalysis1.setExpressionExperimentSetAnalyzed( eeSet );
        eAnalysis1.setName( RandomStringUtils.randomAlphabetic( 6 ) );
        eAnalysis1.setDescription( "An analysis Test 1" );
        eAnalysis1 = analysisService.create( eAnalysis1 );

        // ///////////////
        eAnalysis2 = DifferentialExpressionAnalysis.Factory.newInstance();
        eeSet = initSet();
        eeSet.getExperiments().add( e1 );
        eeSet.getExperiments().add( e2 );
        expressionExperimentSetService.update( eeSet );

        eAnalysis2.setExpressionExperimentSetAnalyzed( eeSet );
        eAnalysis2.setName( RandomStringUtils.randomAlphabetic( 6 ) );
        eAnalysis2.setDescription( "An analysis Test 2" );
        eAnalysis2 = analysisService.create( eAnalysis2 );

        // /////////////
        eAnalysis3 = DifferentialExpressionAnalysis.Factory.newInstance();
        eeSet = initSet();
        eeSet.getExperiments().add( e1 );
        eeSet.getExperiments().add( e2 );
        eeSet.getExperiments().add( e3 );
        expressionExperimentSetService.update( eeSet );

        eAnalysis3.setExpressionExperimentSetAnalyzed( eeSet );
        this.testAnalysisName = RandomStringUtils.randomAlphabetic( 6 );
        eAnalysis3.setName( testAnalysisName );
        eAnalysis3.setDescription( "An analysis Test 3" );
        eAnalysis3 = analysisService.create( eAnalysis3 );

        // ////
        eAnalysis4 = DifferentialExpressionAnalysis.Factory.newInstance();
        eeSet = initSet();
        eeSet.getExperiments().add( e1 );
        eeSet.getExperiments().add( e2 );
        eeSet.getExperiments().add( e3 );
        expressionExperimentSetService.update( eeSet );

        eAnalysis4.setExpressionExperimentSetAnalyzed( eeSet );
        testEESetName = RandomStringUtils.randomAlphabetic( 6 );
        eAnalysis4.setName( testEESetName );
        eAnalysis4.setDescription( "An analysis Test 4" );
        eAnalysis4 = analysisService.create( eAnalysis4 );

    }

    private ExpressionExperimentSet initSet() {
        Taxon mouse = this.getTaxon( "mouse" );

        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setTaxon( mouse );
        eeSet.setName( RandomStringUtils.randomAlphabetic( 6 ) );
        eeSet = expressionExperimentSetService.create( eeSet );
        return eeSet;
    }

    /**
     * 
     */
    @Test
    public void testFindByInvestigation() {

        Collection<DifferentialExpressionAnalysis> results = analysisService.findByInvestigation( e1 );
        assertEquals( 4, results.size() );

        results = analysisService.findByInvestigation( e2 );
        assertEquals( 3, results.size() );

        results = analysisService.findByInvestigation( e3 );
        assertEquals( 2, results.size() );

    }

    /**
     * 
     */
    @Test
    public void testFindByInvestigations() {
        Collection<ExpressionExperiment> investigations = new ArrayList<ExpressionExperiment>();
        investigations.add( e1 );
        investigations.add( e2 );

        Map<Investigation, Collection<DifferentialExpressionAnalysis>> results = analysisService
                .findByInvestigations( investigations );
        assertEquals( 2, results.keySet().size() );

        assertEquals( 4, results.get( e1 ).size() );
        assertEquals( 3, results.get( e2 ).size() );
    }

    @Test
    public void testFindByNameExact() {

        Collection<DifferentialExpressionAnalysis> result = analysisService.findByName( this.testEESetName );
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( this.testEESetName, result.iterator().next().getName() );
    }

    @Test
    public void testFindByNameRecent() {
        Collection<DifferentialExpressionAnalysis> result = analysisService.findByName( this.testAnalysisName );
        assertNotNull( result );
        assertEquals( this.testAnalysisName, result.iterator().next().getName() );
    }

    /**
     *  
     */
    @Test
    public void testFindByUniqueInvestigations() {
        Collection<Investigation> investigations = new ArrayList<Investigation>();
        investigations.add( e1 );
        investigations.add( e2 );

        DifferentialExpressionAnalysis results = analysisService.findByUniqueInvestigations( investigations );
        assertNotNull( results );
        assertEquals( eAnalysis2.getId(), results.getId() );

    }

}
