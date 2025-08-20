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

package ubic.gemma.persistence.service.analysis.expression.diff;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author klc
 */
public class DifferentialExpressionAnalysisServiceTest extends BaseSpringContextTest {

    @Autowired
    private DifferentialExpressionAnalysisService analysisService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private ExpressionExperiment e1;
    private ExpressionExperiment e2;
    private ExpressionExperiment e3;

    private String dea1_name;
    private String dea2_name;

    private String testEESetName;

    private String testAnalysisName;

    @Before
    public void setUp() throws Exception {

        e1 = ExpressionExperiment.Factory.newInstance();
        e1.setShortName( RandomStringUtils.insecure().nextAlphabetic( 6 ) );
        e1 = expressionExperimentService.create( e1 );

        e2 = ExpressionExperiment.Factory.newInstance();
        e2.setShortName( RandomStringUtils.insecure().nextAlphabetic( 6 ) );
        e2 = expressionExperimentService.create( e2 );

        e3 = ExpressionExperiment.Factory.newInstance();
        e3.setShortName( RandomStringUtils.insecure().nextAlphabetic( 6 ) );
        e3 = expressionExperimentService.create( e3 );

        ExpressionExperiment e4 = ExpressionExperiment.Factory.newInstance();
        e4.setShortName( RandomStringUtils.insecure().nextAlphabetic( 6 ) );
        expressionExperimentService.create( e4 );

        // //////////////////
        DifferentialExpressionAnalysis eAnalysis1 = DifferentialExpressionAnalysis.Factory.newInstance();
        eAnalysis1.setExperimentAnalyzed( e1 );
        dea1_name = RandomStringUtils.insecure().nextAlphabetic( 6 );
        eAnalysis1.setName( dea1_name );
        eAnalysis1.setDescription( "An analysis Test 1" );
        analysisService.create( eAnalysis1 );

        // ///////////////
        DifferentialExpressionAnalysis eAnalysis2 = DifferentialExpressionAnalysis.Factory.newInstance();

        eAnalysis2.setExperimentAnalyzed( e2 );
        dea2_name = RandomStringUtils.insecure().nextAlphabetic( 6 );
        eAnalysis2.setName( dea2_name );
        eAnalysis2.setDescription( "An analysis Test 2" );
        analysisService.create( eAnalysis2 );

        // /////////////
        DifferentialExpressionAnalysis eAnalysis3 = DifferentialExpressionAnalysis.Factory.newInstance();

        eAnalysis3.setExperimentAnalyzed( e3 );
        this.testAnalysisName = RandomStringUtils.insecure().nextAlphabetic( 6 );
        eAnalysis3.setName( testAnalysisName );
        eAnalysis3.setDescription( "An analysis Test 3" );
        analysisService.create( eAnalysis3 );

        // ////
        DifferentialExpressionAnalysis eAnalysis4 = DifferentialExpressionAnalysis.Factory.newInstance();
        eAnalysis4.setExperimentAnalyzed( e3 );
        testEESetName = RandomStringUtils.insecure().nextAlphabetic( 6 );
        eAnalysis4.setName( testEESetName );
        eAnalysis4.setDescription( "An analysis Test 4" );
        analysisService.create( eAnalysis4 );

    }

    @After
    public void tearDown() {
        expressionExperimentService.remove( e1 );
        expressionExperimentService.remove( e2 );
        expressionExperimentService.remove( e3 );
    }

    @Test
    public void testFindByInvestigation() {

        Collection<DifferentialExpressionAnalysis> results = analysisService.findByExperiment( e1, true );
        assertEquals( 1, results.size() );
        DifferentialExpressionAnalysis dea = results.iterator().next();
        assertEquals( dea1_name, dea.getName() );

        results = analysisService.findByExperiment( e2, true );
        assertEquals( 1, results.size() );
        dea = results.iterator().next();
        assertEquals( dea2_name, dea.getName() );

        results = analysisService.findByExperiment( e3, true );
        assertEquals( 2, results.size() );
    }

    @Test
    public void testFindByInvestigations() {
        Collection<BioAssaySet> investigations = new ArrayList<>();
        investigations.add( e1 );
        investigations.add( e3 );

        Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> results = analysisService
                .findByExperiments( investigations, true );
        assertEquals( 2, results.keySet().size() );

        assertEquals( 1, results.get( e1 ).size() );
        assertEquals( 2, results.get( e3 ).size() );

        // also by ID
        Map<Long, Collection<DifferentialExpressionAnalysis>> ees = analysisService
                .findByExperimentIds( IdentifiableUtils.getIds( investigations ) );
        assertEquals( 2, ees.size() );

        assertEquals( 1, ees.get( e1.getId() ).size() );
        assertEquals( 2, ees.get( e3.getId() ).size() );
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

}
