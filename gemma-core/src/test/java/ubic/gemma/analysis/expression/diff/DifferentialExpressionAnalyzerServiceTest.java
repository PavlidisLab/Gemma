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

import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerServiceTest extends BaseSpringContextTest {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    private String shortName = "GSE1997";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        differentialExpressionAnalyzerService = ( DifferentialExpressionAnalyzerService ) this
                .getBean( "differentialExpressionAnalyzerService" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

    /**
     * @throws Exception
     */
    public void testGetTopResultsForFactor() throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();

        /* eg. use GSE1077 */
        // Collection<ExpressionAnalysisResult> analysisResults = differentialExpressionAnalyzerService.getTopResults(
        // shortName, 100 );
        /* eg. use GSE1997 */
        Collection<DifferentialExpressionAnalysisResult> analysisResults = differentialExpressionAnalyzerService
                .getTopResultsForFactor( shortName, 100, "protocol", true );

        if ( analysisResults == null ) {
            log.warn( "Could not find analyses for expression experiment with short name " + shortName
                    + ". Expression experiment probably does not exist. Skipping test ..." );
            return;
        }

        for ( DifferentialExpressionAnalysisResult result : analysisResults ) {
            if ( result instanceof ProbeAnalysisResult ) {

                ProbeAnalysisResult presult = ( ProbeAnalysisResult ) result;
                log.info( presult.getProbe().getName() + "; " + presult.getPvalue() );
            } else {
                assertFalse( "Invalid result type. Expected a " + ProbeAnalysisResult.class.getName() + ", received "
                        + result.getClass().getClass().getName(), false );
            }
        }

        assertFalse( analysisResults.isEmpty() );

        watch.stop();

        log.info( "time: " + watch.getTime() );

    }

    /**
     * @throws Exception
     */
    public void testDelete() throws Exception {

        Collection<DifferentialExpressionAnalysisResult> analysisResults = differentialExpressionAnalyzerService
                .getTopResultsForFactor( shortName, 100, "protocol", true );

        if ( analysisResults == null ) {
            log.warn( "Could not find analyses for expression experiment with short name " + shortName
                    + ". Expression experiment probably does not exist. Skipping test ..." );
            return;
        }

        StopWatch watch = new StopWatch();
        watch.start();

        differentialExpressionAnalyzerService.delete( shortName );

        watch.stop();

        log.info( "deletion time: " + watch.getTime() );

    }
}
