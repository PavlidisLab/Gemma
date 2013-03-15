/*
 og* The Gemma project
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

/**
 * Differential expression service to run the differential expression analysis (and persist the results using the
 * appropriate data access objects).
 * <p>
 * Note that there is also a DifferentialExpressionAnalysisService (which handled CRUD for analyses). In contrast this
 * _does_ the analysis.
 * 
 * @author keshav
 * @version $Id$
 */
@Component
public class DifferentialExpressionAnalyzerServiceImpl implements DifferentialExpressionAnalyzerService {

    /**
     * Defines the different types of analyses our linear modeling framework supports:
     * <ul>
     * <li>GENERICLM - genric linear regression (interactions are omitted, but this could change)
     * <li>OSTTEST - one sample t-test
     * <li>OWA - one-way anova
     * <li>TTEST - two sample t-test
     * <li>TWA - two way anova with interaction
     * <li>TWANI - two-way anova with no interaction
     * </ul>
     * 
     * @author Paul
     * @version $Id$
     */
    public enum AnalysisType {
        GENERICLM, OSTTEST /* one-sample */, OWA /* one-way anova */, TTEST, TWA /* with interactions */, TWANI /*
                                                                                                                 * no
                                                                                                                 * interactions
                                                                                                                 */
    }

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalyzerServiceImpl.class );

    @Autowired
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;

    @Autowired
    private AuditTrailService auditTrailService = null;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private DifferentialExpressionAnalysisHelperService helperService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#getAnalyses(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .getAnalyses( expressionExperiment );
        differentialExpressionAnalysisService.thaw( expressionAnalyses );
        return expressionAnalyses;
    }

    /**
     * @param ee
     * @param copyMe
     * @return
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();

        if ( !differentialExpressionAnalysisService.canDelete( copyMe ) ) {
            throw new IllegalArgumentException(
                    "Cannot redo the analysis because it is included in a meta-analysis (or something). "
                            + "Delete the constraining entity first." );
        }

        differentialExpressionAnalysisService.thaw( copyMe );

        log.info( "Will base analysis on old one: " + copyMe );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        if ( copyMe.getSubsetFactorValue() != null ) {
            config.setSubsetFactor( copyMe.getSubsetFactorValue().getExperimentalFactor() );
        }

        Collection<ExpressionAnalysisResultSet> resultSets = copyMe.getResultSets();
        Collection<ExperimentalFactor> factorsFromOldExp = new HashSet<ExperimentalFactor>();
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<ExperimentalFactor> oldfactors = rs.getExperimentalFactors();
            factorsFromOldExp.addAll( oldfactors );

            /*
             * If we included the interaction before, include it again.
             */
            if ( oldfactors.size() == 2 ) {
                log.info( "Including interaction term" );
                config.getInteractionsToInclude().add( oldfactors );
            }

        }

        if ( factorsFromOldExp.isEmpty() ) {
            throw new IllegalStateException( "Old analysis didn't have any factors" );
        }

        config.getFactorsToInclude().addAll( factorsFromOldExp );

        BioAssaySet experimentAnalyzed = copyMe.getExperimentAnalyzed();
        assert experimentAnalyzed != null;

        if ( experimentAnalyzed.equals( ee ) ) {
            results = this.runDifferentialExpressionAnalyses( ee, config );
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet
                && ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment().equals( ee ) ) {
            DifferentialExpressionAnalysis subsetAnalysis = this.runDifferentialExpressionAnalysis(
                    ( ExpressionExperimentSubSet ) experimentAnalyzed, config );

            results.add( subsetAnalysis );
        } else {
            throw new IllegalStateException(
                    "Cannot redo an analysis for one experiment if the analysis is for another (" + ee
                            + " is the proposed target, but analysis is from " + experimentAnalyzed );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#runDifferentialExpressionAnalyses(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = analysisSelectionAndExecutionService
                    .analyze( expressionExperiment, factors );

            diffExpressionAnalyses = persistAnalyses( expressionExperiment, diffExpressionAnalyses, factors );
            /*
             * Save histograms . Do this here, outside of the other transaction .
             */
            for ( DifferentialExpressionAnalysis a : diffExpressionAnalyses ) {
                writeDistributions( expressionExperiment, a );
            }
            return diffExpressionAnalyses;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment,
                    FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                    ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /**
     * @param expressionExperiment
     * @param diffExpressionAnalyses
     * @return
     */
    private Collection<DifferentialExpressionAnalysis> persistAnalyses( ExpressionExperiment expressionExperiment,
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses, Collection<ExperimentalFactor> factors ) {

        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
            DifferentialExpressionAnalysis persistentAnalysis = persistAnalysis( expressionExperiment, analysis,
                    factors );
            results.add( persistentAnalysis );
        }
        return results;
    }

    /**
     * @param expressionExperiment
     * @param newAnalysis
     * @param factors
     * @return
     */
    protected int deleteOldAnalyses( ExpressionExperiment expressionExperiment,
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

                log.info( "Deleting analysis with ID=" + existingAnalysis.getId() );
                deleteAnalysis( expressionExperiment, existingAnalysis );

                numDeleted++;
            }
        }

        if ( numDeleted == 0 ) {
            log.info( "None of the other existing analyses were eligible for deletion" );
        }
        return numDeleted;
    }

    /**
     * @param expressionExperiment
     * @param analysis
     * @param factors
     * @return
     */
    private DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis analysis, Collection<ExperimentalFactor> factors ) {

        deleteOldAnalyses( expressionExperiment, analysis, factors );

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();
        analysis.setResultSets( new HashSet<ExpressionAnalysisResultSet>() );

        DifferentialExpressionAnalysis persistentAnalysis = helperService.persistStub( analysis );

        helperService.addResults( persistentAnalysis, resultSets );

        auditTrailService.addUpdateEvent( expressionExperiment,
                DifferentialExpressionAnalysisEvent.Factory.newInstance(), persistentAnalysis.getDescription()
                        + "; analysis id=" + persistentAnalysis.getId() );

        return persistentAnalysis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#runDifferentialExpressionAnalyses(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment,
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = analysisSelectionAndExecutionService
                    .analyze( expressionExperiment, config );

            diffExpressionAnalyses = persistAnalyses( expressionExperiment, diffExpressionAnalyses,
                    config.getFactorsToInclude() );

            for ( DifferentialExpressionAnalysis a : diffExpressionAnalyses ) {
                writeDistributions( expressionExperiment, a );
            }
            return diffExpressionAnalyses;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment,
                    FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                    ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    @Override
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalysis( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config ) {
        try {

            DifferentialExpressionAnalysis a = analysisSelectionAndExecutionService.analyze( subset, config );

            a = persistAnalysis( subset.getSourceExperiment(), a, config.getFactorsToInclude() );

            writeDistributions( subset.getSourceExperiment(), a );

            return a;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( subset.getSourceExperiment(),
                    FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                    "Failed to run analysis on subset: " + subset, ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#updateScoreDistributionFiles(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void updateScoreDistributionFiles( ExpressionExperiment ee ) {

        Collection<DifferentialExpressionAnalysis> analyses = this.getAnalyses( ee );

        if ( analyses.size() == 0 ) {
            log.info( "No  analyses for experiment " + ee.getShortName()
                    + ".  The differential expression analysis may not have been run on this experiment yet." );
            return;
        }

        for ( DifferentialExpressionAnalysis differentialExpressionAnalysis : analyses ) {
            writeDistributions( ee, differentialExpressionAnalysis );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#updateSummaries(ubic.gemma.model.analysis
     * .expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public void updateSummaries( DifferentialExpressionAnalysis analysis ) {

        DiffExAnalyzer lma = analysisSelectionAndExecutionService.getAnalyzer();
        Map<CompositeSequence, Collection<Gene>> probe2GeneMap = new HashMap<CompositeSequence, Collection<Gene>>();

        log.info( "Reading the analysis ..." );
        differentialExpressionAnalysisService.thaw( analysis );
        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            differentialExpressionResultService.thaw( resultSet );
            List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>(
                    resultSet.getResults() );
            for ( DifferentialExpressionAnalysisResult d : results ) {
                CompositeSequence probe = d.getProbe();
                probe2GeneMap.put( probe, new HashSet<Gene>() );
            }
        }

        log.info( "Retrieving gene-element information ..." );
        probe2GeneMap = compositeSequenceService.getGenes( probe2GeneMap.keySet() );

        if ( probe2GeneMap.isEmpty() ) throw new IllegalStateException( "The probes do not map to genes" );

        int i = 1;
        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            log.info( "Updating stats for " + i + "/" + analysis.getResultSets().size()
                    + " resultsets for the analysis" );
            List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>(
                    resultSet.getResults() );
            Collection<HitListSize> hitlists = lma.computeHitListSizes( results, probe2GeneMap );
            resultSet.getHitListSizes().clear();
            resultSet.getHitListSizes().addAll( hitlists );
            resultSet.setNumberOfGenesTested( lma.getNumberOfGenesTested( results, probe2GeneMap ) );
            resultSet.setNumberOfProbesTested( results.size() );
            differentialExpressionResultService.update( resultSet );
            i++;
        }

        log.info( "Writing distributions" );
        writeDistributions( analysis.getExperimentAnalyzed(), analysis );
        log.info( "Done" );

    }

    public static final String FACTOR_NAME_MANGLING_DELIMITER = "__";

    /**
     * Print the p-value and score distributions. Note that if there are multiple analyses for the experiment, each one
     * will get a set of files.
     * 
     * @param expressionExperiment
     * @param diffExpressionAnalysis - could be on a subset of the experiment.
     */
    private void writeDistributions( BioAssaySet expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {

        /*
         * write histograms
         * 
         * Of 1) pvalues, // (following not used now) 2) scores, 3) qvalues
         * 
         * Put all pvalues in one file etc so we don't get 9 files for a 2x anova with interactions.
         */
        StopWatch timer = new StopWatch();
        timer.start();
        List<Histogram> pvalueHistograms = new ArrayList<Histogram>();
        // List<Histogram> qvalueHistograms = new ArrayList<Histogram>();
        // List<Histogram> scoreHistograms = new ArrayList<Histogram>();

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

            // Histogram qvalHist = new Histogram( factorName, 100, 0.0, 1.0 );
            // qvalueHistograms.add( qvalHist );

            // Histogram scoreHist = new Histogram( factorName, 200, -20, 20 );
            // scoreHistograms.add( scoreHist );

            for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {
                // Double correctedPvalue = result.getCorrectedPvalue();
                // if ( correctedPvalue != null ) qvalHist.fill( correctedPvalue );

                Double pvalue = result.getPvalue();
                if ( pvalue != null ) pvalHist.fill( pvalue );
            }

        }

        DoubleMatrix<String, String> pvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        // DoubleMatrix<String, String> qvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size()
        // );
        // DoubleMatrix<String, String> scoreDists = new DenseDoubleMatrix<String, String>( 200, resultSetList.size() );

        fillDists( factorNames, pvalueHistograms, pvalueDists );
        // fillDists( factorNames, qvalueHistograms, qvalueDists );
        // fillDists( factorNames, scoreHistograms, scoreDists );

        saveDistributionMatrixToFile( "pvalues", pvalueDists, expressionExperiment, resultSetList );
        // saveDistributionMatrixToFile( "qvalues", qvalueDists, expressionExperiment, resultSetList );
        // saveDistributionMatrixToFile( "scores", scoreDists, expressionExperiment, resultSetList );

        if ( timer.getTime() > 5000 ) {
            log.info( "Done writing distributions: " + timer.getTime() + "ms" );
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

    /**
     * @param expressionExperiment
     * @return the directory where the files should be written.
     */
    private File prepareDirectoryForDistributions( BioAssaySet expressionExperiment ) {
        if ( expressionExperiment instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet ss = ( ExpressionExperimentSubSet ) expressionExperiment;
            ExpressionExperiment source = ss.getSourceExperiment();

            File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( FileTools.cleanForFileName( source
                    .getShortName() ) + ".Subset" + ss.getId() );
            FileTools.createDir( dir.toString() );
            return dir;
        } else if ( expressionExperiment instanceof ExpressionExperiment ) {
            File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( FileTools
                    .cleanForFileName( ( ( ExpressionExperiment ) expressionExperiment ).getShortName() ) );
            FileTools.createDir( dir.toString() );
            return dir;
        } else {
            throw new IllegalStateException( "Cannot handle bioassay sets of type=" + expressionExperiment.getClass() );
        }

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
            BioAssaySet expressionExperiment, List<ExpressionAnalysisResultSet> resultSetList ) {

        Long analysisId = resultSetList.iterator().next().getAnalysis().getId();

        File f = prepareDirectoryForDistributions( expressionExperiment );

        String shortName = null;

        if ( expressionExperiment instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet ss = ( ExpressionExperimentSubSet ) expressionExperiment;
            ExpressionExperiment source = ss.getSourceExperiment();
            shortName = source.getShortName();
        } else if ( expressionExperiment instanceof ExpressionExperiment ) {
            shortName = ( ( ExpressionExperiment ) expressionExperiment ).getShortName();
        } else {
            throw new IllegalStateException( "Cannot handle bioassay sets of type=" + expressionExperiment.getClass() );
        }

        String histFileName = FileTools.cleanForFileName( shortName ) + ".an" + analysisId + "." + extraSuffix
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
            out.write( ExpressionDataFileService.DISCLAIMER );
            out.write( "# exp=" + expressionExperiment.getId() + " " + shortName + " " + expressionExperiment.getName()
                    + " \n" );

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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#deleteOldAnalysis(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment,
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public void deleteAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis existingAnalysis ) {
        log.info( "Deleting old differential expression analysis for experiment " + expressionExperiment.getShortName()
                + " Analysis ID=" + existingAnalysis.getId() );
        differentialExpressionAnalysisService.delete( existingAnalysis );

        deleteStatistics( expressionExperiment, existingAnalysis );
    }

    /**
     * Delete any flat files that might have been generated.
     * 
     * @param expressionExperiment
     * @param de
     */
    private void deleteAnalysisFiles( DifferentialExpressionAnalysis analysis ) {
        try {
            expressionDataFileService.deleteDiffExArchiveFile( analysis );
        } catch ( IOException e ) {
            log.error( "Error during deletion of old files for analyses to be deleted: " + e.getMessage() );
        }
    }

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#deleteOldAnalyses(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @Override
    public int deleteAnalyses( ExpressionExperiment expressionExperiment ) {
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

            deleteStatistics( expressionExperiment, de );
            deleteAnalysisFiles( de );
            result++;
        }

        return result;
    }

    /**
     * Remove old files which will otherwise be cruft.
     * 
     * @param ee
     * @param analysis
     */
    public void deleteStatistics( ExpressionExperiment ee, DifferentialExpressionAnalysis analysis ) {

        File f = prepareDirectoryForDistributions( ee );

        String histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "."
                + "pvalues" + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        File oldf = new File( f, histFileName );
        if ( oldf.exists() && oldf.canWrite() ) {
            if ( !oldf.delete() ) {
                log.warn( "Could not delete: " + oldf );
            }
        }

        histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "qvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() && oldf.canWrite() ) {

            if ( !oldf.delete() ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
        histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "scores"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() && oldf.canWrite() ) {
            if ( !oldf.delete() ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
    }

}
