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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    ExpressionExperiment ee = null;

    private String shortName = "GSE1997";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {
        ee = expressionExperimentService.findByShortName( shortName );
        if ( ee != null ) expressionExperimentService.thawLite( ee );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {

        if ( ee == null ) return;

        Collection<ExpressionAnalysisResultSet> resultSets = differentialExpressionAnalyzerService.getResultSets( ee );

        log.info( "Result sets for " + shortName + resultSets.size() );

        ExpressionAnalysisResultSet rs = resultSets.iterator().next();
        Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

        if ( results == null || results.isEmpty() ) return;

        StopWatch watch = new StopWatch();
        watch.start();

        differentialExpressionAnalyzerService.deleteOldAnalyses( ee );

        watch.stop();

        log.info( "deletion time: " + watch.getTime() );

    }

    /**
     * 
     */
    @Test
    public void testWritePValuesHistogram() {

        if ( ee == null ) return;

        Exception ex = null;
        try {
            differentialExpressionAnalyzerService.updateScoreDistributionFiles( ee );
        } catch ( IOException e ) {
            ex = e;
            e.printStackTrace();
        } finally {
            assertTrue( ex == null );
        }

    }
}
