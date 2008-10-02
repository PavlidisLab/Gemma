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
package ubic.gemma.analysis.expression.diff;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerServiceTest extends BaseSpringContextTest {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    private ExpressionExperimentService expressionExperimentService = null;

    ExpressionExperiment ee = null;

    private String shortName = "GSE1997";

    /*
     * (non-Javadoc)
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        differentialExpressionAnalyzerService = ( DifferentialExpressionAnalyzerService ) this
                .getBean( "differentialExpressionAnalyzerService" );

        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        ee = expressionExperimentService.findByShortName( shortName );

        if ( ee != null ) expressionExperimentService.thawLite( ee );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.testing.BaseSpringContextTest#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

    /**
     * @throws Exception
     */
    public void testDelete() throws Exception {

        if ( ee == null ) return;

        Collection<ExpressionAnalysisResultSet> resultSets = differentialExpressionAnalyzerService.getResultSets( ee );

        log.info( "Result sets for " + shortName + resultSets.size() );

        ExpressionAnalysisResultSet rs = resultSets.iterator().next();
        Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

        if ( results == null || results.isEmpty() ) return;

        StopWatch watch = new StopWatch();
        watch.start();

        differentialExpressionAnalyzerService.delete( shortName );

        watch.stop();

        log.info( "deletion time: " + watch.getTime() );

    }

    /**
     * 
     */
    public void testWritePValuesHistogram() {

        if ( ee == null ) return;

        Exception ex = null;
        try {
            differentialExpressionAnalyzerService.writePValuesHistogram( ee );
        } catch ( IOException e ) {
            ex = e;
            e.printStackTrace();
        } finally {
            assertTrue( ex == null );
        }

    }
}
