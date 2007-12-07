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

package ubic.gemma.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.AnalysisService;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 * @version $Id$
 */
public class AnalysisServiceTest extends BaseSpringContextTest {

    private AnalysisService analysisS;
    private ExpressionExperimentService eeS;

    // Test Data
    Analysis eAnalysis1;
    Analysis eAnalysis2;
    Analysis eAnalysis3;
    Analysis eAnalysis4;

    ExpressionExperiment e1;
    ExpressionExperiment e2;
    ExpressionExperiment e3;

    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        e1 = ExpressionExperiment.Factory.newInstance();
        e1.setName( "test e1" );
        e1 = eeS.create( e1 );

        e2 = ExpressionExperiment.Factory.newInstance();
        e2.setName( "test e2" );
        e2 = eeS.create( e2 );

        e3 = ExpressionExperiment.Factory.newInstance();
        e3.setName( "test e3" );
        e3 = eeS.create( e3 );

        Collection<Investigation> investigations = new HashSet<Investigation>();

        eAnalysis1 = ExpressionAnalysis.Factory.newInstance();
        investigations.add( e1 );
      //  eAnalysis1.setAnalyzedInvestigation( investigations );
        eAnalysis1.setName( "TestAnalysis1" );
        eAnalysis1.setDescription( "An analysis Test 1" );
        eAnalysis1 = analysisS.create( eAnalysis1 );

        eAnalysis2 = ExpressionAnalysis.Factory.newInstance();
        investigations = new HashSet<Investigation>();
        investigations.add( e1 );
        investigations.add( e2 );
     //   eAnalysis2.setAnalyzedInvestigation( investigations );
        eAnalysis2.setName( "TestAnalysis2" );
        eAnalysis2.setDescription( "An analysis Test 2" );
        eAnalysis2 = analysisS.create( eAnalysis2 );

        eAnalysis4 = ExpressionAnalysis.Factory.newInstance();
        investigations = new HashSet<Investigation>();
        investigations.add( e1 );
        investigations.add( e2 );
        investigations.add( e3 );

    //.setAnalyzedInvestigation( investigations );
        eAnalysis4.setName( "Test" );
        eAnalysis4.setDescription( "An analysis Test 4" );
        eAnalysis4 = analysisS.create( eAnalysis4 );

        eAnalysis3 = ExpressionAnalysis.Factory.newInstance();
        investigations = new HashSet<Investigation>();
        investigations.add( e1 );
        investigations.add( e2 );
        investigations.add( e3 );
      //.setAnalyzedInvestigation( investigations );
        eAnalysis3.setName( "TestAnalysis3" );
        eAnalysis3.setDescription( "An analysis Test 3" );
        eAnalysis3 = analysisS.create( eAnalysis3 );

    }

    /**
     * 
     */
    public void testFindByInvestigations() {
        Collection<Investigation> investigations = new ArrayList<Investigation>();
        investigations.add( e1 );
        investigations.add( e2 );

        Map results = analysisS.findByInvestigations( investigations );
        assertEquals( 3, results.keySet().size() );

    }

    /**
     * 
     */
    public void testFindByInvestion() {

        Collection results = analysisS.findByInvestigation( e1 );
        assertEquals( 4, results.size() );

        results = analysisS.findByInvestigation( e2 );
        assertEquals( 3, results.size() );

        results = analysisS.findByInvestigation( e3 );
        assertEquals( 2, results.size() );

    }

    /**
     * 
     */
    public void testFindByUniqueInvestigations() {
        Collection<Investigation> investigations = new ArrayList<Investigation>();
        investigations.add( e1 );
        investigations.add( e2 );

        Analysis results = analysisS.findByUniqueInvestigations( investigations );
        assertEquals( e2.getId(), results.getId() );

    }

    public void testFindByNameExact() {

        Analysis result = analysisS.findByName( "Test" );
        assertEquals( "Test", result.getName() );
    }

    public void testFindByNameRecent() {

        Analysis result = analysisS.findByName( "TestA" );
        assertEquals( "TestAnalysis3", result.getName() );
    }

    /**
     * @param analysisS the analysisS to set
     */
    public void setAnalysisService( AnalysisService analysisS ) {
        this.analysisS = analysisS;
    }

    /**
     * @param ees the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService ees ) {
        this.eeS = ees;
    }

}
