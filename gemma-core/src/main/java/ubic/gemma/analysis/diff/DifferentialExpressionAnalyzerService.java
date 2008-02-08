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

import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.DifferentialExpressionAnalysisResultComparator;

/**
 * A spring loaded differential expression service to run the differential expression analysis (and persist the results
 * using the appropriate data access objects).
 * 
 * @spring.bean id="differentialExpressionAnalyzerService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name="differentialExpressionAnalyzer" ref="differentialExpressionAnalyzer"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerService {

    private static final String ONE_WAY_ANOVA = "one";
    private static final String TWO_WAY_ANOVA = "two";
    private static final int NUM_OWA_RESULT_SETS = 1;// num one way anova sets
    private static final int NUM_TWA_FACTORS = 2;// num two way anova factors

    private Log log = LogFactory.getLog( this.getClass() );
    private ExpressionExperimentService expressionExperimentService = null;
    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer = null;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    private PersisterHelper persisterHelper = null;

    /**
     * Finds the persistent expression experiment. If there are no associated analyses with this experiment, the
     * differential expression analysis is first run and persisted if forceAnalysis = true and then returned.
     * 
     * @param expressionExperiment
     * @param forceAnalysis
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysis> getDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, boolean forceAnalysis ) {

        boolean analysisRun = runDifferentialExpressionAnalysis( expressionExperiment, forceAnalysis );
        if ( !analysisRun ) {
            return null;
        }

        Collection<ExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        DifferentialExpressionAnalysis diffExpressionAnalysis = differentialExpressionAnalyzer.getExpressionAnalysis();

        if ( diffExpressionAnalysis == null ) {
            log.error( "No differential expression analyses for " + expressionExperiment.getShortName() );
            return null;
        }

        Collection<ExpressionExperiment> experimentsAnalyzed = diffExpressionAnalysis.getExperimentsAnalyzed();
        experimentsAnalyzed.add( expressionExperiment );
        diffExpressionAnalysis.setExperimentsAnalyzed( experimentsAnalyzed );

        diffExpressionAnalysis = ( DifferentialExpressionAnalysis ) persisterHelper.persist( diffExpressionAnalysis );
        expressionAnalyses.add( diffExpressionAnalysis );

        differentialExpressionAnalysisService.thaw( expressionAnalyses );

        /* return the expression analyses of type differential expression */
        Collection<DifferentialExpressionAnalysis> differentialExpressionAnalyses = new HashSet<DifferentialExpressionAnalysis>();
        for ( ExpressionAnalysis ea : expressionAnalyses ) {
            if ( ea instanceof DifferentialExpressionAnalysis ) {
                DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) ea;
                differentialExpressionAnalyses.add( dea );
            }
        }

        return differentialExpressionAnalyses;
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment} if analyses do not already exist. If forceRun =
     * true, run even if analyses exist.
     * 
     * @param expressionExperiment
     * @param forceRun
     * @return boolean Whether analysis was run or not. This will be false if analysis had already been run on this
     *         experiment and forceRun=false.
     */
    @SuppressWarnings("unchecked")
    public boolean runDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment, boolean forceRun ) {

        boolean analysisRun = false;

        Collection<ExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );
        if ( forceRun || expressionAnalyses.isEmpty() || !wasDifferentialAnalysisRun( expressionExperiment ) ) {

            String message = "Analyze " + expressionExperiment.getShortName() + ".  ";

            if ( forceRun ) {
                message = message + "Force analysis (re-analyze even if analysis was previously run)? " + forceRun
                        + ".  ";
            }

            if ( expressionAnalyses.isEmpty() ) {
                message = message + "Experiment " + expressionExperiment.getShortName()
                        + " does not have any associated analyses.  ";
            }

            if ( !wasDifferentialAnalysisRun( expressionExperiment ) ) {
                message = message + "Experiment " + expressionExperiment.getShortName()
                        + " does not have any associated differential expression data.  ";
            }

            message = message + "Running analysis and persisting results.  This may take some time.";

            log.warn( message );

            differentialExpressionAnalyzer.analyze( expressionExperiment );
            analysisRun = true;
        } else {
            boolean hasDiffex = false;
            for ( ExpressionAnalysis expressionAnalysis : expressionAnalyses ) {
                if ( expressionAnalysis instanceof DifferentialExpressionAnalysis ) {
                    hasDiffex = true;
                }
            }
            if ( hasDiffex ) {
                log.warn( "Differential expression analysis already run for experiment "
                        + expressionExperiment.getShortName()
                        + ".  Not running again.  To force a re-analysis, set forceRun = true." );
                analysisRun = false;
            }
        }
        return analysisRun;
    }

    /**
     * @param ee
     * @param forceAnalysis
     * @return
     */
    private DifferentialExpressionAnalysis getDifferentialExpressionAnalysis( ExpressionExperiment ee,
            boolean forceAnalysis, String analysisType ) {
        Collection<DifferentialExpressionAnalysis> analyses = this
                .getDifferentialExpressionAnalyses( ee, forceAnalysis );
        if ( analyses.isEmpty() ) {
            if ( !forceAnalysis ) {
                log.error( "No analyses associated with experiment: " + ee.getShortName() );
                return null;
            }
            boolean analysisRun = runDifferentialExpressionAnalysis( ee, forceAnalysis );
            if ( !analysisRun ) {
                return null;
            }
            analyses = this.getDifferentialExpressionAnalyses( ee, forceAnalysis );
        }

        DifferentialExpressionAnalysis analysis = getDifferentialExpressionAnalysisFromAnalyses( analyses, analysisType );

        if ( analysis == null ) {
            if ( !forceAnalysis ) {
                log.error( "Differential expression analysis not found for experiment " + ee.getShortName()
                        + ".  Returning ..." );

                return null;
            }
            log.debug( "Was told to run analyis.  Running now ... " );
            this.runDifferentialExpressionAnalysis( ee, forceAnalysis );
            analysis = getDifferentialExpressionAnalysisFromAnalyses( analyses, analysisType );
        }

        return analysis;
    }

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
    public Collection<DifferentialExpressionAnalysisResult> getTopResults( String shortName, int top,
            boolean forceAnalysis ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find expeiment with name: " + shortName + ".  Returning ..." );
            return null;
        }
        expressionExperimentService.thawLite( ee );

        return getTopResults( ee, top, forceAnalysis );
    }

    /**
     * Returns the top results of a one way anova for the experiment.
     * 
     * @param top
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysisResult> getTopResults( ExpressionExperiment ee, int top,
            boolean forceAnalysis ) {

        ExpressionAnalysis analysis = getDifferentialExpressionAnalysis( ee, forceAnalysis, ONE_WAY_ANOVA );
        if ( analysis == null ) return null;

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        log.info( "Getting one way anova results for experiment: " + ee.getShortName() );

        if ( resultSets.size() != NUM_OWA_RESULT_SETS )
            throw new RuntimeException( "Invalid number of result sets for analysis for experiment: "
                    + ee.getShortName() );
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        Collection<DifferentialExpressionAnalysisResult> analysisResults = resultSet.getResults();

        top = setTopLimit( ee.getShortName(), top, analysisResults );

        List<DifferentialExpressionAnalysisResult> topResults = sortResults( top, analysisResults );

        return topResults;
    }

    /**
     * Returns the top results of a two way anova for an experiment with shortName and the given factor. If the analysis
     * has already been run, the analysis is re-run only if forceAnalysis is true.
     * 
     * @param shortName
     * @param top
     * @param factorName
     * @param forceAnalysis
     * @return
     */
    public Collection<DifferentialExpressionAnalysisResult> getTopResultsForFactor( String shortName, int top,
            String factorName, boolean forceAnalysis ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find expeiment with name: " + shortName + ".  Returning ..." );
            return null;
        }
        expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        ExperimentalFactor factorToUse = null;
        for ( ExperimentalFactor factor : factors ) {
            if ( StringUtils.contains( factor.getName(), factorName ) ) {
                factorToUse = factor;
                break;
            }
        }

        if ( factorToUse == null ) {
            log.error( "No matching factor.  Returning ..." );
            return null;
        }

        return getTopResultsForFactor( ee, factorToUse, top, forceAnalysis );
    }

    /**
     * Returns the top results of a two way anova for an experiment with shortName and the given factor. If the analysis
     * has already been run, the analysis is re-run only if forceAnalysis is true.
     * 
     * @param ee
     * @param factor
     * @param top
     * @param forceAnalysis
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysisResult> getTopResultsForFactor( ExpressionExperiment ee,
            ExperimentalFactor factor, int top, boolean forceAnalysis ) {

        ExpressionAnalysis analysis = getDifferentialExpressionAnalysis( ee, forceAnalysis, TWO_WAY_ANOVA );
        if ( analysis == null ) return null;

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        log.info( "Getting two way anova results for experiment: " + ee.getShortName() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            Collection<ExperimentalFactor> factorFromResultSet = resultSet.getExperimentalFactor();
            for ( ExperimentalFactor f : factorFromResultSet ) {
                if ( factor.equals( f ) ) {
                    log.info( "Returning top results for factor with" + "\'" + factor.getName() + "\'"
                            + " in the name (or description)." );
                    Collection<DifferentialExpressionAnalysisResult> analysisResults = resultSet.getResults();
                    top = setTopLimit( ee.getShortName(), top, analysisResults );
                    List<DifferentialExpressionAnalysisResult> topResults = sortResults( top, analysisResults );
                    return topResults;
                }
            }
        }
        return null;
    }

    /**
     * Returns the top interaction results for a two way anova (with interactions).
     * 
     * @param ee
     * @param factorA
     * @param factorB
     * @param top
     * @param forceAnalysis
     * @return
     */
    public Collection<DifferentialExpressionAnalysisResult> getTopInteractionResults( String shortName, int top,
            boolean forceAnalysis ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find expeiment with name: " + shortName + ".  Returning ..." );
            return null;
        }
        expressionExperimentService.thawLite( ee );

        return this.getTopInteractionResults( ee, top, forceAnalysis );
    }

    /**
     * Returns the top interaction results for a two way anova (with interactions).
     * 
     * @param ee
     * @param factors
     * @param top
     * @param forceAnalysis
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysisResult> getTopInteractionResults( ExpressionExperiment ee, int top,
            boolean forceAnalysis ) {
        ExpressionAnalysis analysis = getDifferentialExpressionAnalysis( ee, forceAnalysis, TWO_WAY_ANOVA );
        if ( analysis == null ) return null;

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        log.info( "Getting two way anova results for experiment: " + ee.getShortName() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            Collection<ExperimentalFactor> factorsFromResult = resultSet.getExperimentalFactor();
            // FIXME this is kind of cheating by only checking the size
            if ( factorsFromResult.size() == NUM_TWA_FACTORS ) {
                log.info( "Returning top interaction results for factors " + "\'" + factorsFromResult.iterator().next()
                        + "\'" + " and \'" + factorsFromResult.iterator().next() + "\'"
                        + " in the name (or description)." );
                Collection<DifferentialExpressionAnalysisResult> analysisResults = resultSet.getResults();
                top = setTopLimit( ee.getShortName(), top, analysisResults );
                List<DifferentialExpressionAnalysisResult> topResults = sortResults( top, analysisResults );
                return topResults;
            }
        }
        log.error( "No interaction results found for " + ee.getShortName() + ".  Returning null ..." );
        return null;
    }

    // TODO add getResultsForProbe

    /**
     * Returns true if differential expression data exists for the experiment, else false.
     * 
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee ) {

        boolean wasRun = false;

        Collection<ExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        for ( ExpressionAnalysis ea : expressionAnalyses ) {
            if ( ea instanceof DifferentialExpressionAnalysis ) {
                wasRun = true;
                break;
            }
        }

        return wasRun;
    }

    /**
     * Delete the differential expression analysis for the experiment with shortName.
     * 
     * @param shortName
     */
    @SuppressWarnings("unchecked")
    public void delete( String shortName ) {
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.info( "Experiment with name " + shortName
                    + " does not exist and therefore has no accociated analyses to remove." );
            return;
        }
        delete( ee );
    }

    /**
     * Delete the differential expression analysis for the experiment.
     * 
     * @param expressionExperiment
     */
    @SuppressWarnings("unchecked")
    public void delete( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> diffAnalysis = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            Long toDelete = de.getId();
            differentialExpressionAnalysisService.delete( toDelete );
        }
    }

    /**
     * @param analyses
     * @param analysis
     * @param analysisType
     * @return
     */
    private DifferentialExpressionAnalysis getDifferentialExpressionAnalysisFromAnalyses(
            Collection<DifferentialExpressionAnalysis> analyses, String analysisType ) {
        DifferentialExpressionAnalysis analysis = null;
        for ( DifferentialExpressionAnalysis a : analyses ) {
            analysis = a;
            break;
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
    private List<DifferentialExpressionAnalysisResult> sortResults( int top,
            Collection<DifferentialExpressionAnalysisResult> analysisResults ) {
        // FIXME This is a silly hack. Return a list with analysis.getAnalysisResults since you know this has to be
        // sorted.
        DifferentialExpressionAnalysisResult[] analysisResultsAsArray = analysisResults
                .toArray( new DifferentialExpressionAnalysisResult[analysisResults.size()] );
        List<DifferentialExpressionAnalysisResult> analysisResultsAsList = Arrays.asList( analysisResultsAsArray );
        // end fixme

        Collections.sort( analysisResultsAsList, DifferentialExpressionAnalysisResultComparator.Factory.newInstance() );

        Iterator<DifferentialExpressionAnalysisResult> iter = analysisResultsAsList.iterator();

        List<DifferentialExpressionAnalysisResult> topResults = new ArrayList<DifferentialExpressionAnalysisResult>();

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
    private int setTopLimit( String shortName, int top, Collection<DifferentialExpressionAnalysisResult> analysisResults ) {
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
    public void setDifferentialExpressionAnalyzer( DifferentialExpressionAnalyzer differentialExpressionAnalyzer ) {
        this.differentialExpressionAnalyzer = differentialExpressionAnalyzer;
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

}
