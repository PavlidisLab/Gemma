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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * A spring loaded differential expression service to run the differential expression analysis (and persist the results
 * using the appropriate data access objects).
 * 
 * @spring.bean id="differentialExpressionAnalyzerService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name="differentialExpressionAnalysisResultService" ref="differentialExpressionAnalysisResultService"
 * @spring.property name="differentialExpressionAnalyzer" ref="differentialExpressionAnalyzer"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerService {

    private Log log = LogFactory.getLog( this.getClass() );
    private ExpressionExperimentService expressionExperimentService = null;
    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer = null;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService = null;
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

        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        Collection<BioAssaySet> experimentsAnalyzed = new HashSet<BioAssaySet>();
        experimentsAnalyzed.add( expressionExperiment );
        eeSet.setExperiments( experimentsAnalyzed );
        diffExpressionAnalysis.setExpressionExperimentSetAnalyzed( eeSet );

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
     * true, runs even if analyses exist but first deletes the old differential expression analysis.
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
                delete( expressionExperiment );
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
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionAnalysisResultSet> getResultSets( ExpressionExperiment expressionExperiment ) {
        return differentialExpressionAnalysisService.getResultSets( expressionExperiment );
    }

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

        if ( diffAnalysis == null || diffAnalysis.isEmpty() ) {
            log.info( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return;
        }

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            Long toDelete = de.getId();

            log.info( "Deleting existing differential expression analysis for experiment "
                    + expressionExperiment.getShortName() );
            differentialExpressionAnalysisService.delete( toDelete );
        }
    }

    /**
     * @param experiments
     */
    public void writePValuesHistogram( ExpressionExperiment ee ) throws IOException {

        String sep = "_";

        Collection<ExpressionAnalysisResultSet> resultSets = this.getResultSets( ee );

        if ( resultSets.size() == 0 ) {
            log.info( "No result sets for experiment " + ee.getShortName()
                    + ".  The differential expression analysis may not have been run on this experiment yet." );
        } else {
            log.info( "Result sets for " + ee.getShortName() + ": " + resultSets.size() );
        }

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            differentialExpressionAnalysisResultService.thaw( resultSet );

            String factorNames = StringUtils.EMPTY;
            Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactor();
            if ( factors.size() > 1 ) {
                for ( ExperimentalFactor f : factors ) {
                    factorNames += f.getName() + sep;
                }
                factorNames = StringUtils.removeEnd( factorNames, sep );
            } else {
                factorNames = factors.iterator().next().getName();
            }
            Collection<DifferentialExpressionAnalysisResult> results = resultSet.getResults();

            this.writePValuesHistogram( results, ee, factorNames );
        }
    }

    /**
     * @param results
     * @param expressionExperiment
     * @param factorNames
     */
    public void writePValuesHistogram( Collection<DifferentialExpressionAnalysisResult> results,
            ExpressionExperiment expressionExperiment, String factorNames ) throws IOException {

        File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( expressionExperiment.getShortName() );

        FileTools.createDir( dir.toString() );

        String histFileName = null;
        if ( factorNames != null ) {
            histFileName = expressionExperiment.getShortName() + "_" + factorNames
                    + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        } else {
            histFileName = expressionExperiment.getShortName() + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        }

        Histogram hist = generateHistogram( histFileName, 100, 0, 1, results );

        String path = dir + File.separator + hist.getName();

        File outputFile = new File( path );
        if ( outputFile.exists() ) {
            outputFile.delete();
        }

        FileWriter out = new FileWriter( outputFile );
        out.write( "# Differential Expression distribution\n" );
        out.write( "# date=" + ( new Date() ) + "\n" );
        out.write( "# exp=" + expressionExperiment + " " + expressionExperiment.getShortName() + "\n" );
        out.write( "Bin\tCount\n" );
        hist.writeToFile( out );
        out.close();

    }

    /**
     * Generates a histogram for the given results.
     * 
     * @param histFileName
     * @param numBins
     * @param min
     * @param max
     * @param results
     * @return
     */
    private Histogram generateHistogram( String histFileName, int numBins, int min, int max,
            Collection<DifferentialExpressionAnalysisResult> results ) {

        Histogram hist = new Histogram( histFileName, numBins, min, max );
        for ( DifferentialExpressionAnalysisResult r : results ) {
            hist.fill( r.getPvalue() );
        }

        return hist;
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

    /**
     * @param differentialExpressionAnalysisService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param persisterHelper
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param differentialExpressionAnalysisResultService
     */
    public void setDifferentialExpressionAnalysisResultService(
            DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService ) {
        this.differentialExpressionAnalysisResultService = differentialExpressionAnalysisResultService;
    }
}
