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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * A spring loaded differential expression service to run the differential expression analysis (and persist the results
 * using the appropriate data access objects).
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class DifferentialExpressionAnalyzerService {

    public enum AnalysisType {
        OWA, TTEST, TWA, TWIA
    }

    @Autowired
    private AuditTrailService auditTrailService = null;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private PersisterHelper persisterHelper = null;

    /**
     * Delete the differential expression analysis for the experiment with shortName.
     * 
     * @param shortName
     */
    public void delete( String shortName ) {
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.info( "Experiment with name " + shortName
                    + " does not exist and therefore has no accociated analyses to remove." );
            return;
        }
        deleteOldAnalyses( ee );
    }

    /**
     * Delete the differential expression analysis for the experiment.
     * 
     * @param expressionExperiment
     */
    public void deleteOldAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> diffAnalysis = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        if ( diffAnalysis == null || diffAnalysis.isEmpty() ) {
            log.info( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return;
        }

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            log.info( "Deleting old differential expression analysis for experiment "
                    + expressionExperiment.getShortName() );
            differentialExpressionAnalysisService.delete( de );
        }
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment}.
     * 
     * @param expressionExperiment
     */
    public DifferentialExpressionAnalysis doDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment );
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment} using the given factor(s)
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    public DifferentialExpressionAnalysis doDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment, factors );
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment} using the given factor(s) and analysis type.
     * 
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public DifferentialExpressionAnalysis doDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment, factors, type );
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    public Collection<ExpressionAnalysisResultSet> getResultSets( ExpressionExperiment expressionExperiment ) {
        return differentialExpressionAnalysisService.getResultSets( expressionExperiment );
    }

    /**
     * Run the differential expression analysis. First deletes the old differential expression analysis, if any.
     * 
     * @param expressionExperiment
     * @return
     */
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment ) {

        DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis( expressionExperiment );

        deleteOldAnalyses( expressionExperiment );

        return persistAnalysis( expressionExperiment, diffExpressionAnalysis );
    }

    /**
     * Run the differential expression analysis. First deletes the old differential expression analysis, if any.
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {
        DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis( expressionExperiment,
                factors );

        deleteOldAnalyses( expressionExperiment );

        return persistAnalysis( expressionExperiment, diffExpressionAnalysis );
    }

    /**
     * Runs the differential expression analysis, then deletes the old differential expression analysis (if any).
     * 
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {

        DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis( expressionExperiment,
                factors, type );

        deleteOldAnalyses( expressionExperiment );

        return persistAnalysis( expressionExperiment, diffExpressionAnalysis );
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param differentialExpressionAnalysisService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param differentialExpressionResultService the differentialExpressionResultService to set
     */
    public void setDifferentialExpressionResultService(
            DifferentialExpressionResultService differentialExpressionResultService ) {
        this.differentialExpressionResultService = differentialExpressionResultService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param persisterHelper
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Returns true if differential expression data exists for the experiment, else false.
     * 
     * @param ee
     * @return
     */
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee ) {

        boolean wasRun = false;

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
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

            differentialExpressionResultService.thaw( resultSet );

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

    private DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {
        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        Collection<BioAssaySet> experimentsAnalyzed = new HashSet<BioAssaySet>();
        experimentsAnalyzed.add( expressionExperiment );
        eeSet.setExperiments( experimentsAnalyzed );
        diffExpressionAnalysis.setExpressionExperimentSetAnalyzed( eeSet );

        diffExpressionAnalysis = ( DifferentialExpressionAnalysis ) persisterHelper.persist( diffExpressionAnalysis );

        /*
         * Audit event!
         */
        auditTrailService.addUpdateEvent( expressionExperiment, DifferentialExpressionAnalysisEvent.Factory
                .newInstance(), diffExpressionAnalysis.getDescription() );
        return diffExpressionAnalysis;
    }

}
