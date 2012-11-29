/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

import org.apache.commons.lang.time.StopWatch;
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
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.Persister;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
@Service
public class DifferentialExpressionAnalysisHelperServiceImpl implements DifferentialExpressionAnalysisHelperService {

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private Persister persisterHelper = null;

    @Autowired
    private AuditTrailService auditTrailService = null;

    /**
     * @param expressionExperiment
     * @param diffExpressionAnalyses
     * @return
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> persistAnalyses( ExpressionExperiment expressionExperiment,
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses, Collection<ExperimentalFactor> factors ) {

        for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
            deleteOldAnalyses( expressionExperiment, analysis, factors );
        }

        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
            DifferentialExpressionAnalysis persistentAnalysis = persistAnalysis( expressionExperiment, analysis,
                    factors );
            results.add( persistentAnalysis );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisHelperService#persistAnalysis(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment,
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis, Collection<ExperimentalFactor> factors ) {

        deleteOldAnalyses( expressionExperiment, diffExpressionAnalysis, factors );

        assert expressionExperiment.getId() != null;

        StopWatch timer = new StopWatch();
        timer.start();
        log.info( "Saving new results" );
        DifferentialExpressionAnalysis savedResults = ( DifferentialExpressionAnalysis ) persisterHelper
                .persist( diffExpressionAnalysis );

        /*
         * Audit event
         */
        auditTrailService.addUpdateEvent( expressionExperiment,
                DifferentialExpressionAnalysisEvent.Factory.newInstance(), savedResults.getDescription()
                        + "; analysis id=" + savedResults.getId() );

        /*
         * Save histograms
         */
        writeDistributions( expressionExperiment, savedResults );

        /*
         * Update the report
         */
        expressionExperimentReportService.generateSummary( expressionExperiment.getId() );

        if ( timer.getTime() > 10000 ) {
            log.info( "Done saving analysis in " + timer.getTime() + "ms" );
        }

        return savedResults;
    }

    /**
     * Print the p-value and score distributions. Note that if there are multiple analyses for the experiment, each one
     * will get a set of files.
     * 
     * @param expressionExperiment
     * @param diffExpressionAnalysis - could be on a subset of the experiment.
     */
    @Override
    public void writeDistributions( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {

        /*
         * write histograms
         * 
         * Of 1) pvalues, 2) scores, 3) qvalues
         * 
         * Put all pvalues in one file etc so we don't get 9 files for a 2x anova with interactions.
         */
        StopWatch timer = new StopWatch();
        timer.start();
        List<Histogram> pvalueHistograms = new ArrayList<Histogram>();
        List<Histogram> qvalueHistograms = new ArrayList<Histogram>();
        List<Histogram> scoreHistograms = new ArrayList<Histogram>();

        List<ExpressionAnalysisResultSet> resultSetList = new ArrayList<ExpressionAnalysisResultSet>();
        resultSetList.addAll( diffExpressionAnalysis.getResultSets() );

        List<String> factorNames = new ArrayList<String>();

        for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {
            resultSet = differentialExpressionResultService.thaw( resultSet );
            // FIXME might not be available.
            String factorName = "";

            // these will be headings on the
            for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
                // Make a unique column heading.
                factorName = factorName + ( factorName.equals( "" ) ? "" : ":" ) + factor.getName()
                        + FACTOR_NAME_MANGLING_DELIMITER + factor.getId();
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

                /*
                 * FIXME do contrasts?
                 */
                // Double effectSize = result.getEffectSize();
                // if ( effectSize != null ) scoreHist.fill( effectSize );

                Double pvalue = result.getPvalue();
                if ( pvalue != null ) pvalHist.fill( pvalue );
            }

        }

        DoubleMatrix<String, String> pvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        DoubleMatrix<String, String> qvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        DoubleMatrix<String, String> scoreDists = new DenseDoubleMatrix<String, String>( 200, resultSetList.size() );

        fillDists( factorNames, pvalueHistograms, pvalueDists );
        fillDists( factorNames, qvalueHistograms, qvalueDists );
        fillDists( factorNames, scoreHistograms, scoreDists );

        saveDistributionMatrixToFile( "pvalues", pvalueDists, expressionExperiment, resultSetList );
        saveDistributionMatrixToFile( "qvalues", qvalueDists, expressionExperiment, resultSetList );
        saveDistributionMatrixToFile( "scores", scoreDists, expressionExperiment, resultSetList );

        if ( timer.getTime() > 5000 ) {
            log.info( "Done writing distributions: " + timer.getTime() + "ms" );
        }
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

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#deleteOldAnalyses(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment,
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis, java.util.Collection)
     */
    @Override
    public int deleteOldAnalyses( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis newAnalysis, Collection<ExperimentalFactor> factors ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );
        int numDeleted = 0;
        if ( diffAnalyses == null || diffAnalyses.isEmpty() ) {
            log.info( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return numDeleted;
        }

        this.differentialExpressionAnalysisService.thaw( diffAnalyses );

        for ( DifferentialExpressionAnalysis existingAnalysis : diffAnalyses ) {

            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();

            for ( ExpressionAnalysisResultSet resultSet : existingAnalysis.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            FactorValue subsetFactorValueForExisting = existingAnalysis.getSubsetFactorValue();

            /*
             * Match if: factors are the same, and if this is a subset, it's the same subset factorvalue.
             */
            if ( factorsInAnalysis.size() == factors.size()
                    && factorsInAnalysis.containsAll( factors )
                    && ( subsetFactorValueForExisting == null || subsetFactorValueForExisting.equals( newAnalysis
                            .getSubsetFactorValue() ) ) ) {

                deleteOldAnalysis( expressionExperiment, existingAnalysis );

                numDeleted++;
            }
        }

        if ( numDeleted == 0 ) {
            log.info( "None of the existing analyses were eligible for deletion" );
        }
        return numDeleted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#deleteOldAnalyses(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @Override
    public int deleteOldAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> diffAnalysis = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        int result = 0;
        if ( diffAnalysis == null || diffAnalysis.isEmpty() ) {
            log.debug( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return result;
        }

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            log.info( "Deleting old differential expression analysis for experiment "
                    + expressionExperiment.getShortName() + ": Analysis ID=" + de.getId() );
            differentialExpressionAnalysisService.delete( de );

            deleteOldDistributionMatrices( expressionExperiment, de );
            result++;
        }

        /*
         * Delete old flat files.
         */
        expressionDataFileService.deleteDiffExFile( expressionExperiment );

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#deleteOldAnalysis(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment,
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public void deleteOldAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis existingAnalysis ) {
        log.info( "Deleting old differential expression analysis for experiment " + expressionExperiment.getShortName()
                + " Analysis ID=" + existingAnalysis.getId() );
        differentialExpressionAnalysisService.delete( existingAnalysis );

        /*
         * Delete old flat files. This deletes them all, could be fixed but not a big deal.
         */
        expressionDataFileService.deleteDiffExFile( expressionExperiment );

        /*
         * Delete the old statistic distributions.
         */
        deleteOldDistributionMatrices( expressionExperiment, existingAnalysis );
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

        String histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "."
                + "pvalues" + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        File oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }

        histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "qvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
        histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "scores"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
    }

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

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

        String histFileName = FileTools.cleanForFileName( expressionExperiment.getShortName() ) + ".an" + analysisId
                + "." + extraSuffix + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        File outputFile = new File( f, histFileName );

        // log.info( outputFile );
        try {
            FileWriter out = new FileWriter( outputFile, true /*
                                                               * clobber, but usually won't since file names are per
                                                               * analyssi
                                                               */);
            out.write( "# Gemma: differential expression statistics - " + extraSuffix + "\n" );
            out.write( "# Generated=" + ( new Date() ) + "\n" );
            out.write( ExpressionDataFileService.DISCLAIMER );
            out.write( "# exp=" + expressionExperiment.getId() + " " + expressionExperiment.getShortName() + " "
                    + expressionExperiment.getName() + " \n" );

            for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {
                BioAssaySet analyzedSet = resultSet.getAnalysis().getExperimentAnalyzed();

                if ( analyzedSet instanceof ExpressionExperimentSubSet ) {
                    out.write( "# SubSet id=" + analyzedSet.getId() + " " + analyzedSet.getName() + " "
                            + analyzedSet.getDescription() + "\n" );
                }

                Long resultSetId = resultSet.getId();
                out.write( "# ResultSet id=" + resultSetId + ", analysisId=" + analysisId + "\n" );

            }

            MatrixWriter<String, String> writer = new MatrixWriter<String, String>( out );
            writer.writeMatrix( histograms, true );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param factorNames
     * @param histograms
     * @param dists
     */
    private void fillDists( List<String> factorNames, List<Histogram> histograms, DoubleMatrix<String, String> dists ) {

        assert !histograms.isEmpty();

        int i = 0;
        for ( Histogram h : histograms ) {
            if ( i == 0 ) {
                dists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                dists.setColumnNames( factorNames );
            }

            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                dists.set( j, i, binHeights[j] );

            }
            i++;
        }
    }

}
