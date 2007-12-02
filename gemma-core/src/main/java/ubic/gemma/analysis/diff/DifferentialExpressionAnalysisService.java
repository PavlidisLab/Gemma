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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.ExpressionAnalysisResultComparator;

/**
 * A spring loaded differential expression service to run the differential expression analysis (and persist the results
 * using the appropriate data access objects).
 * 
 * @spring.bean id="differentialExpressionAnalysisService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="differentialExpressionAnalysis" ref="differentialExpressionAnalysis"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisService {

    private Log log = LogFactory.getLog( this.getClass() );
    ExpressionExperimentService expressionExperimentService = null;

    DifferentialExpressionAnalysis differentialExpressionAnalysis = null;

    /**
     * Returns the top persistent analysis results for the experiment with shortName. If the expression experiment given
     * by shortName is not found, returns null.
     * 
     * @param shortName
     * @param top
     * @return
     */
    public Collection<ExpressionAnalysisResult> getTopExpressionAnalysisResults( String shortName, int top ) {
        // FIXME you need to differentiate between different analyses for a given experiment ... by name?

        Collection<ExpressionAnalysis> analyses = this.getPersistentExpressionAnalyses( shortName );
        if ( analyses == null ) return null;

        // FIXME for now, as a test, just do something stupid and use the first analysis.

        ExpressionAnalysis analysis = analyses.iterator().next();

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        Collection<ExpressionAnalysisResult> analysisResults = resultSet.getResults();

        if ( top > analysisResults.size() ) {
            log.warn( "Number of desired results, " + top
                    + ", is greater than the number of analysis results for experiment with short name " + shortName
                    + ".  Will return all results." );
            top = analysisResults.size();
        }

        // FIXME This is a silly hack. Return a list with analysis.getAnalysisResults since you know this has to be
        // sorted (don't want to edit the model just yet).
        ExpressionAnalysisResult[] analysisResultsAsArray = analysisResults
                .toArray( new ExpressionAnalysisResult[analysisResults.size()] );
        List<ExpressionAnalysisResult> analysisResultsAsList = Arrays.asList( analysisResultsAsArray );
        // end fixme

        Collections.sort( analysisResultsAsList, ExpressionAnalysisResultComparator.Factory.newInstance() );

        Iterator<ExpressionAnalysisResult> iter = analysisResultsAsList.iterator();

        List<ExpressionAnalysisResult> topResults = new ArrayList<ExpressionAnalysisResult>();

        for ( int i = 0; i < top; i++ ) {
            topResults.add( iter.next() );
        }

        return topResults;
    }

    /**
     * Finds the persistent expression experiment. If there are no associated analyses with this experiment, the
     * differential expression analysis is first run, the analysis is persisted and then returned.
     * 
     * @param expressionExperiment
     * @return
     */
    public Collection<ExpressionAnalysis> getPersistentExpressionAnalyses( ExpressionExperiment expressionExperiment ) {

        Collection<ExpressionAnalysis> expressionAnalyses = expressionExperiment.getExpressionAnalyses();
        if ( expressionAnalyses == null || expressionAnalyses.isEmpty() ) {
            log
                    .warn( "Experiment "
                            + expressionExperiment.getShortName()
                            + " does not have any associated analyses.  Running differenial expression analysis and persisting results.  This may take some time." );

            expressionAnalyses = new HashSet<ExpressionAnalysis>();

            differentialExpressionAnalysis.analyze( expressionExperiment );

            ExpressionAnalysis expressionAnalysis = differentialExpressionAnalysis.getExpressionAnalysis();

            Collection<ExpressionExperiment> experimentsAnalyzed = new HashSet<ExpressionExperiment>();
            experimentsAnalyzed.add( expressionExperiment );

            expressionAnalysis.setExperimentsAnalyzed( experimentsAnalyzed );

            expressionAnalyses.add( expressionAnalysis );

            expressionExperiment.setExpressionAnalyses( expressionAnalyses );
            expressionExperimentService.update( expressionExperiment );

        }

        return expressionAnalyses;
    }

    /**
     * Finds the persistent expression experiment by the shortName and returns the analyses. If the expression
     * experiment does not exist, returns null.
     * 
     * @param shortName
     * @return
     */
    public Collection<ExpressionAnalysis> getPersistentExpressionAnalyses( String shortName ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );

        expressionExperimentService.thaw( ee );

        if ( ee == null ) return null;

        return this.getPersistentExpressionAnalyses( ee );

    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param differentialExpressionAnalysis
     */
    public void setDifferentialExpressionAnalysis( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.differentialExpressionAnalysis = differentialExpressionAnalysis;
    }
}
