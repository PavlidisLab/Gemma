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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
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

    private static final String DIFFERENTIAL_EXPRESSION = "differential";
    private static final String ONE_WAY_ANOVA = "one";
    private static final String TWO_WAY_ANOVA = "two";
    private static final int NUM_RESULT_SETS_OWA = 1;// num one way anova sets

    private Log log = LogFactory.getLog( this.getClass() );
    private ExpressionExperimentService expressionExperimentService = null;
    private DifferentialExpressionAnalysis differentialExpressionAnalysis = null;

    /**
     * Returns the top results of a one way anova for the experiment with shortName.
     * <p>
     * If the expression experiment given by shortName is not found, returns null.
     * <p>
     * If there are no analyses for the given experiment, returns null.
     * 
     * @param shortName
     * @param top
     * @return
     */
    public Collection<ExpressionAnalysisResult> getTopResults( String shortName, int top ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find expeiment with name: " + shortName + ".  Returning ..." );
            return null;
        }
        expressionExperimentService.thawLite( ee );

        Collection<ExpressionAnalysis> analyses = this.getPersistentExpressionAnalyses( ee );
        if ( analyses == null ) {
            log.error( "No analyses associated with experiment: " + shortName );
            return null;
        }

        ExpressionAnalysis analysis = confirmAnalysisExists( analyses, ONE_WAY_ANOVA );

        if ( analysis == null ) {
            log.error( "One way anova differential expression analysis not found for experiment " + shortName
                    + ".  Returning ..." );
            return null;
        }

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        log.info( "Getting one way anova results for experiment: " + shortName );

        if ( resultSets.size() != NUM_RESULT_SETS_OWA )
            throw new RuntimeException( "Invalid number of result sets for analysis for experiment: " + shortName );

        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        Collection<ExpressionAnalysisResult> analysisResults = resultSet.getResults();

        top = setTopLimit( shortName, top, analysisResults );

        List<ExpressionAnalysisResult> topResults = sortResults( top, analysisResults );

        return topResults;
    }

    /**
     * Returns the top results of a two way anova for an experiment with shortName and the given factor.
     * 
     * @param shortName
     * @param top
     * @param factor
     * @return
     */
    public Collection<ExpressionAnalysisResult> getTopResultsForFactor( String shortName, int top, String factorName ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find expeiment with name: " + shortName + ".  Returning ..." );
            return null;
        }
        expressionExperimentService.thawLite( ee );

        Collection<ExpressionAnalysis> analyses = this.getPersistentExpressionAnalyses( ee );
        if ( analyses == null ) {
            log.error( "No analyses associated with experiment: " + shortName );
            return null;
        }

        ExpressionAnalysis analysis = confirmAnalysisExists( analyses, TWO_WAY_ANOVA );

        if ( analysis == null ) {
            log.error( "Two way anova differential expression analysis not found for experiment " + shortName
                    + ".  Returning ..." );
            return null;
        }

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        log.info( "Getting two way anova results for experiment: " + shortName );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            ExperimentalFactor factor = resultSet.getExperimentalFactor();
            if ( factor == null ) {
                log.debug( "Null experimental factor for result set.  Skipping ..." );
                continue;
            }
            if ( StringUtils.contains( factor.getName(), factorName ) ) {
                log.info( "Returning top results for factor with" + "\'" + factorName + "\'"
                        + " in the name (or description)." );
                Collection<ExpressionAnalysisResult> analysisResults = resultSet.getResults();
                top = setTopLimit( shortName, top, analysisResults );
                List<ExpressionAnalysisResult> topResults = sortResults( top, analysisResults );
                return topResults;
            }

        }

        return null;
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
     * @param analyses
     * @param analysis
     * @param analysisType
     * @return
     */
    private ExpressionAnalysis confirmAnalysisExists( Collection<ExpressionAnalysis> analyses, String analysisType ) {
        ExpressionAnalysis analysis = null;
        for ( ExpressionAnalysis a : analyses ) {

            if ( StringUtils.equalsIgnoreCase( a.getName().toLowerCase(), DIFFERENTIAL_EXPRESSION.toLowerCase() )
                    || StringUtils.contains( a.getDescription().toLowerCase(), DIFFERENTIAL_EXPRESSION.toLowerCase() ) ) {

                if ( StringUtils.contains( a.getName().toLowerCase(), analysisType.toLowerCase() )
                        || StringUtils.contains( a.getDescription().toLowerCase(), analysisType.toLowerCase() ) ) {
                    analysis = a;
                    break;
                }
            }
        }
        return analysis;
    }

    /**
     * The analysisResults are sorted.
     * 
     * @param top
     * @param analysisResults
     * @return
     */
    private List<ExpressionAnalysisResult> sortResults( int top, Collection<ExpressionAnalysisResult> analysisResults ) {
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
     * If the number of desired results (top) is greater than the number of analysis results, all results are returned.
     * 
     * @param shortName
     * @param top
     * @param analysisResults
     * @return
     */
    private int setTopLimit( String shortName, int top, Collection<ExpressionAnalysisResult> analysisResults ) {
        if ( top > analysisResults.size() ) {
            log.warn( "Number of desired results, " + top
                    + ", is greater than the number of analysis results for experiment with short name " + shortName
                    + ".  Will return all results." );
            top = analysisResults.size();
        }
        return top;
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
