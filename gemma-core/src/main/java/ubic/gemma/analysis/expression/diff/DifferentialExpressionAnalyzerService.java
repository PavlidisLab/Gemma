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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Differential expression service to run the differential expression analysis (and persist the results using the
 * appropriate data access objects).
 * <p>
 * FIXME this is very confusingly named as there is also a DifferentialExpressionAnalysisService and a
 * DifferentialExpressionAnalyzer
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class DifferentialExpressionAnalyzerService {

    public enum AnalysisType {
        GENERICLM, OSTTEST, OWA, TTEST, TWA, TWIA
    }

    @Autowired
    private AuditTrailService auditTrailService = null;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private PersisterHelper persisterHelper = null;

    /**
     * Delete all the differential expression analyses for the experiment with shortName.
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
     * Delete all the differential expression analyses for the experiment.
     * 
     * @param expressionExperiment
     */
    public void deleteOldAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> diffAnalysis = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        if ( diffAnalysis == null || diffAnalysis.isEmpty() ) {
            log.debug( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return;
        }

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            log.info( "Deleting old differential expression analysis for experiment "
                    + expressionExperiment.getShortName() );
            differentialExpressionAnalysisService.delete( de );

            deleteOldDistributionMatrices( expressionExperiment, de );
        }
    }

    /**
     * Delete all the differential expression analyses for the experiment that use the given set of factors.
     * 
     * @param expressionExperiment
     * @param factors
     */
    public void deleteOldAnalyses( ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        if ( diffAnalyses == null || diffAnalyses.isEmpty() ) {
            log.info( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return;
        }

        this.differentialExpressionAnalysisService.thaw( diffAnalyses );

        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {

            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();

            for ( ExpressionAnalysisResultSet resultSet : de.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            if ( factorsInAnalysis.size() == factors.size() && factorsInAnalysis.containsAll( factors ) ) {

                log.info( "Deleting old differential expression analysis for experiment "
                        + expressionExperiment.getShortName() );
                differentialExpressionAnalysisService.delete( de );

                /*
                 * Delete the old statistic distributions.
                 */
                deleteOldDistributionMatrices( expressionExperiment, de );
            }
        }
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment}.
     * 
     * @param expressionExperiment
     */
    public DifferentialExpressionAnalysis doDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();
        if ( experimentalFactors.size() > MAX_FACTORS_FOR_AUTO_ANALYSIS ) {
            throw new IllegalArgumentException( "Has more than " + MAX_FACTORS_FOR_AUTO_ANALYSIS
                    + " factors, please specify the factors" );
        }

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
     * @param expressionExperiment
     * @param config
     * @return
     */
    public DifferentialExpressionAnalysis doDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment, config );
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

    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {
        try {
            DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis(
                    expressionExperiment, config );

            deleteOldAnalyses( expressionExperiment );

            return persistAnalysis( expressionExperiment, diffExpressionAnalysis );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    private static final int MAX_FACTORS_FOR_AUTO_ANALYSIS = 3;

    /**
     * Run the differential expression analysis, attempting to identify the appropriate analysis automatically. First
     * deletes ALL the old differential expression analyses for this experiment, if any.
     * 
     * @param expressionExperiment
     * @return
     */
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment ) {

        try {
            DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis( expressionExperiment );

            deleteOldAnalyses( expressionExperiment );

            return persistAnalysis( expressionExperiment, diffExpressionAnalysis );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /**
     * Run the differential expression analysis. First deletes the matching existing differential expression analysis,
     * if any.
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {
        try {
            DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis(
                    expressionExperiment, factors );

            deleteOldAnalyses( expressionExperiment, factors );

            return persistAnalysis( expressionExperiment, diffExpressionAnalysis );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /**
     * Runs the differential expression analysis, then deletes the matching old differential expression analysis (if
     * any).
     * 
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalyses( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {

        try {
            DifferentialExpressionAnalysis diffExpressionAnalysis = doDifferentialExpressionAnalysis(
                    expressionExperiment, factors, type );

            deleteOldAnalyses( expressionExperiment, factors );

            DifferentialExpressionAnalysis analysis = persistAnalysis( expressionExperiment, diffExpressionAnalysis );

            return analysis;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }

    }

    /**
     * @param ee
     * @throws IOException
     */
    public void updateScoreDistributionFiles( ExpressionExperiment ee ) throws IOException {

        Collection<ExpressionAnalysisResultSet> resultSets = this.getResultSets( ee );

        if ( resultSets.size() == 0 ) {
            log.info( "No result sets for experiment " + ee.getShortName()
                    + ".  The differential expression analysis may not have been run on this experiment yet." );
            return;
        }

        /*
         * Organize by analysis.
         */
        Set<DifferentialExpressionAnalysis> analyses = new HashSet<DifferentialExpressionAnalysis>();

        log.info( "Diff analyses for " + ee.getShortName() + ": " + analyses.size() );

        for ( ExpressionAnalysisResultSet expressionAnalysisResultSet : resultSets ) {
            analyses.add( expressionAnalysisResultSet.getAnalysis() );
        }

        for ( DifferentialExpressionAnalysis differentialExpressionAnalysis : analyses ) {
            writeDistributions( ee, differentialExpressionAnalysis );
        }

    }

    /**
     * Returns true if any differential expression data exists for the experiment, else false.
     * 
     * @param ee
     * @return
     */
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee ) {

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        return !expressionAnalyses.isEmpty();
    }

    /**
     * Returns true if differential expression data exists for the experiment with the given factors, else false.
     * 
     * @param ee
     * @param factors
     * @return
     */
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee, Collection<ExperimentalFactor> factors ) {

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        for ( DifferentialExpressionAnalysis de : expressionAnalyses ) {
            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();
            for ( ExpressionAnalysisResultSet resultSet : de.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            if ( factorsInAnalysis.size() == factors.size() && factorsInAnalysis.containsAll( factors ) ) {
                return true;
            }

        }
        return false;
    }

    /**
     * Print the p-value and score distributions
     * 
     * @param expressionExperiment
     * @param diffExpressionAnalysis
     */
    protected void writeDistributions( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {

        /*
         * write histograms
         * 
         * Of 1) pvalues, 2) scores, 3) qvalues
         * 
         * Put all pvalues in one file etc so we don't get 9 files for a 2x anova with interactions.
         */

        List<Histogram> pvalueHistograms = new ArrayList<Histogram>();
        List<Histogram> qvalueHistograms = new ArrayList<Histogram>();
        List<Histogram> scoreHistograms = new ArrayList<Histogram>();

        List<ExpressionAnalysisResultSet> resultSetList = new ArrayList<ExpressionAnalysisResultSet>();
        resultSetList.addAll( diffExpressionAnalysis.getResultSets() );

        List<String> factorNames = new ArrayList<String>();

        for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {

            String factorName = "";

            // these will be headings on the
            for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
                factorName = factorName + ( factorName.equals( "" ) ? "" : ":" ) + factor.getName();
            }
            factorNames.add( factorName );

            Histogram pvalHist = new Histogram( factorName, 100, 0.0, 1.0 );
            pvalueHistograms.add( pvalHist );

            Histogram qvalHist = new Histogram( factorName, 100, 0.0, 1.0 );
            qvalueHistograms.add( qvalHist );

            Histogram scoreHist = new Histogram( factorName, 200, -20, 20 );
            scoreHistograms.add( scoreHist );

            for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {
                Double correctedPvalue = result.getCorrectedPvalue();
                if ( correctedPvalue != null ) qvalHist.fill( correctedPvalue );

                Double effectSize = result.getEffectSize();
                if ( effectSize != null ) scoreHist.fill( effectSize );

                Double pvalue = result.getPvalue();
                if ( pvalue != null ) pvalHist.fill( pvalue );
            }

        }

        DoubleMatrix<String, String> pvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        DoubleMatrix<String, String> qvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        DoubleMatrix<String, String> scoreDists = new DenseDoubleMatrix<String, String>( 200, resultSetList.size() );

        int i = 0;
        for ( Histogram h : pvalueHistograms ) {
            if ( i == 0 ) {
                pvalueDists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                pvalueDists.setColumnNames( factorNames );

            }

            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                pvalueDists.set( j, i, binHeights[j] );

            }
            i++;
        }
        i = 0;
        for ( Histogram h : qvalueHistograms ) {
            if ( i == 0 ) {
                qvalueDists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                qvalueDists.setColumnNames( factorNames );
            }
            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                qvalueDists.set( j, i, binHeights[j] );
            }
            i++;
        }
        i = 0;
        for ( Histogram h : scoreHistograms ) {
            if ( i == 0 ) {
                scoreDists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                scoreDists.setColumnNames( factorNames );
            }
            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                scoreDists.set( j, i, binHeights[j] );
            }
            i++;
        }

        saveDistributionMatrixToFile( "pvalues", pvalueDists, expressionExperiment, resultSetList );
        saveDistributionMatrixToFile( "qvalues", qvalueDists, expressionExperiment, resultSetList );
        saveDistributionMatrixToFile( "scores", scoreDists, expressionExperiment, resultSetList );

    }

    /**
     * Remove old files which will otherwise be cruft.
     * 
     * @param ee
     * @param analysis
     */
    private void deleteOldDistributionMatrices( ExpressionExperiment ee, DifferentialExpressionAnalysis analysis ) {

        File f = prepareDirectoryForDistributions( ee );

        boolean deleted = false;

        String histFileName = cleanShortName( ee ) + ".an" + analysis.getId() + "." + "pvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        File oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }

        histFileName = cleanShortName( ee ) + ".an" + analysis.getId() + "." + "qvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
        histFileName = cleanShortName( ee ) + ".an" + analysis.getId() + "." + "scores"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
    }

    /**
     * @param expressionExperiment
     * @param diffExpressionAnalysis
     * @return
     */
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

        /*
         * Save histograms
         */
        writeDistributions( expressionExperiment, diffExpressionAnalysis );

        /*
         * Update the report
         */
        expressionExperimentReportService.generateSummaryObject( expressionExperiment.getId() );

        return diffExpressionAnalysis;
    }

    /**
     * @param expressionExperiment
     * @return the directory where the files should be written.
     */
    private File prepareDirectoryForDistributions( ExpressionExperiment expressionExperiment ) {
        File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( expressionExperiment.getShortName() );
        FileTools.createDir( dir.toString() );
        return dir;
    }

    /**
     * Avoid getting file names with spaces etc.
     * 
     * @param ee
     * @return
     */
    private String cleanShortName( ExpressionExperiment ee ) {
        return ee.getShortName().replaceAll( "[\\s\'\";,]", "_" );
    }

    /**
     * Print the distributions to a file.
     * 
     * @param extraSuffix eg qvalue, pvalue, score
     * @param histograms each column is a histogram
     * @param expressionExperiment
     * @param resulstsets in the same order as columns in the
     */
    private void saveDistributionMatrixToFile( String extraSuffix, DoubleMatrix<String, String> histograms,
            ExpressionExperiment expressionExperiment, List<ExpressionAnalysisResultSet> resultSetList ) {

        Long analysisId = resultSetList.iterator().next().getAnalysis().getId();

        File f = prepareDirectoryForDistributions( expressionExperiment );

        String histFileName = cleanShortName( expressionExperiment ) + ".an" + analysisId + "." + extraSuffix
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        File outputFile = new File( f, histFileName );

        // log.info( outputFile );
        try {
            FileWriter out = new FileWriter( outputFile, true /*
                                                               * clobber, but usually won't since file names are per
                                                               * analyssi
                                                               */);
            out.write( "# Gemma: differential expression statistics - " + extraSuffix + "\n" );
            out.write( "# Generated=" + ( new Date() ) + "\n" );
            out.write( "# If you use this file for your research, please cite the Gemma web site\n" );
            out.write( "# exp=" + expressionExperiment.getId() + " " + expressionExperiment.getShortName() + " "
                    + expressionExperiment.getName() + " \n" );

            for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {
                Long resultSetId = resultSet.getId();
                out.write( "# ResultSet id=" + resultSetId + ", analysisId=" + analysisId + "\n" );
            }

            MatrixWriter<String, String> writer = new MatrixWriter<String, String>( out );
            writer.writeMatrix( histograms, true );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }
}
