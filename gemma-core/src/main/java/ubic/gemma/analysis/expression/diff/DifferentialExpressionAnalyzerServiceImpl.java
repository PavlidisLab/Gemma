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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
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
import ubic.gemma.model.analysis.expression.diff.PvalueDistribution;
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
     * <li>GENERICLM - generic linear regression (interactions are omitted, but this could change)
     * <li>OSTTEST - one sample t-test
     * <li>OWA - one-way ANOVA
     * <li>TTEST - two sample t-test
     * <li>TWO_WAY_ANOVA_WITH_INTERACTION
     * <li>TWO_WAY_ANOVA_NO_INTERACTION
     * </ul>
     * 
     * @author Paul
     * @version $Id$
     */
    public enum AnalysisType {
        GENERICLM, OSTTEST /* one-sample */, OWA /* one-way ANOVA */, TTEST, TWO_WAY_ANOVA_WITH_INTERACTION /*
                                                                                                             * with
                                                                                                             * interactions
                                                                                                             */, TWO_WAY_ANOVA_NO_INTERACTION /*
                                                                                                                                               * no
                                                                                                                                               * interactions
                                                                                                                                               */
    }

    public static final String FACTOR_NAME_MANGLING_DELIMITER = "__";

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalyzerServiceImpl.class );

    private static final int MINIMUM_NUMBER_OF_HITS_TO_SAVE = 50;

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
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private DifferentialExpressionAnalysisHelperService helperService;

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
        try {
            expressionDataFileService.deleteDiffExArchiveFile( existingAnalysis );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
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

        // histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "qvalues"
        // + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        // oldf = new File( f, histFileName );
        // if ( oldf.exists() && oldf.canWrite() ) {
        // if ( !oldf.delete() ) {
        // log.warn( "Could not delete: " + oldf );
        // }
        // }
        //
        // histFileName = FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "scores"
        // + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        // oldf = new File( f, histFileName );
        // if ( oldf.exists() && oldf.canWrite() ) {
        // if ( !oldf.delete() ) {
        // log.warn( "Could not delete: " + oldf );
        // }
        // }
    }

    /*
     * 
     */
    @Override
    public Collection<ExpressionAnalysisResultSet> extendAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis toUpdate ) {

        /*
         * One way to do this is redo without saving, and then copy the results over to the given result sets that
         * match. But that requires matching up old and new result sets.
         */
        differentialExpressionAnalysisService.thaw( toUpdate );
        DifferentialExpressionAnalysisConfig config = copyConfig( toUpdate, null );

        assert config.getQvalueThreshold() == null;

        Collection<DifferentialExpressionAnalysis> results = redoWithoutSave( ee, toUpdate, config );

        /*
         * Match up old and new...
         */

        extendResultSets( results, toUpdate.getResultSets() );
        return toUpdate.getResultSets();

    }

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
     * Made public for testing purposes only.
     * 
     * @param expressionExperiment
     * @param analysis
     * @param config
     * @return
     */
    @Override
    public DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis analysis, DifferentialExpressionAnalysisConfig config ) {

        deleteOldAnalyses( expressionExperiment, analysis, config.getFactorsToInclude() );
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        analysis.setResultSets( new HashSet<ExpressionAnalysisResultSet>() );

        // first transaction, gets us an ID
        DifferentialExpressionAnalysis persistentAnalysis = helperService.persistStub( analysis );

        // second set of transactions creates the empty resultSets.
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

            rs.setResults( new HashSet<DifferentialExpressionAnalysisResult>() );
            ExpressionAnalysisResultSet prs = helperService.create( rs );
            assert prs != null;
            for ( DifferentialExpressionAnalysisResult r : results ) {
                r.setResultSet( prs );
            }
            analysis.getResultSets().add( prs );
            rs.getResults().addAll( results );

            prs.setQvalueThresholdForStorage( config.getQvalueThreshold() );
            addPvalueDistribution( prs );

        }

        // we do this here because now we have IDs for everything.
        expressionDataFileService.getDiffExpressionAnalysisArchiveFile( expressionExperiment, analysis, resultSets );

        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            removeUnwantedResults( config.getQvalueThreshold(), rs.getResults() );
        }

        // third transaction - add results.
        log.info( "Saving results" );
        helperService.addResults( persistentAnalysis, resultSets );

        // final transaction: audit.
        try {
            auditTrailService.addUpdateEvent( expressionExperiment,
                    DifferentialExpressionAnalysisEvent.Factory.newInstance(), persistentAnalysis.getDescription()
                            + "; analysis id=" + persistentAnalysis.getId() );
        } catch ( Exception e ) {
            log.error( "Error while trying to add audit event: " + e.getMessage(), e );
            log.error( "Continuing ..." );
            /*
             * We shouldn't fail completely due to this.
             */
        }

        if ( timer.getTime() > 5000 ) {
            log.info( "Save results: " + timer.getTime() + "ms" );
        }

        return persistentAnalysis;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#redoAnalysis(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe ) {
        return this.redoAnalysis( ee, copyMe, DifferentialExpressionAnalysisConfig.DEFAULT_QVALUE_THRESHOLD );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#redoAnalysis(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis,
     * java.lang.Double)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe, Double qValueThreshold ) {

        if ( !differentialExpressionAnalysisService.canDelete( copyMe ) ) {
            throw new IllegalArgumentException(
                    "Cannot redo the analysis because it is included in a meta-analysis (or something). "
                            + "Delete the constraining entity first." );
        }

        differentialExpressionAnalysisService.thaw( copyMe );

        log.info( "Will base analysis on old one: " + copyMe );
        DifferentialExpressionAnalysisConfig config = copyConfig( copyMe, qValueThreshold );

        Collection<DifferentialExpressionAnalysis> results = redoWithoutSave( ee, copyMe, config );

        /*
         * Finally, persist it.
         */
        return persistAnalyses( ee, results, config );
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

            diffExpressionAnalyses = persistAnalyses( expressionExperiment, diffExpressionAnalyses, config );

            return diffExpressionAnalyses;
        } catch ( Exception e ) {
            log.error( "Error during differential expression analysis: " + e.getMessage(), e );
            try {
                auditTrailService.addUpdateEvent( expressionExperiment,
                        FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                        ExceptionUtils.getStackTrace( e ) );
            } catch ( Exception e2 ) {
                log.error( "Could not attach failure audit event" );
            }
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

        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            writeDistributions( ee, this.differentialExpressionAnalysisService.thawFully( analysis ) );
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

        if ( probe2GeneMap.isEmpty() ) throw new IllegalStateException( "The elements do not map to genes" );

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
     * @param resultSet
     * @return
     */
    private Histogram addPvalueDistribution( ExpressionAnalysisResultSet resultSet ) {
        Histogram pvalHist = new Histogram( "", 100, 0.0, 1.0 );

        for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {

            Double pvalue = result.getPvalue();
            if ( pvalue != null ) pvalHist.fill( pvalue );
        }

        PvalueDistribution pvd = PvalueDistribution.Factory.newInstance();
        pvd.setNumBins( 100 );
        ByteArrayConverter bac = new ByteArrayConverter();
        pvd.setBinCounts( bac.doubleArrayToBytes( pvalHist.getArray() ) );
        resultSet.setPvalueDistribution( pvd ); // do not save yet.
        return pvalHist;
    }

    /**
     * @param temprs
     * @param oldrs
     * @return
     */
    private boolean configsAreEqual( ExpressionAnalysisResultSet temprs, ExpressionAnalysisResultSet oldrs ) {
        return temprs.getBaselineGroup().equals( oldrs.getBaselineGroup() )
                && temprs.getExperimentalFactors().size() == oldrs.getExperimentalFactors().size()
                && temprs.getExperimentalFactors().containsAll( oldrs.getExperimentalFactors() );
    }

    /**
     * @param copyMe
     * @param qValueThreshold
     * @return
     */
    private DifferentialExpressionAnalysisConfig copyConfig( DifferentialExpressionAnalysis copyMe,
            Double qValueThreshold ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setQvalueThreshold( qValueThreshold ); // this might be the same as the original, or not.

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
        return config;
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

    /**
     * @param oldrs
     * @param temprs
     */
    private void extendResultSet( ExpressionAnalysisResultSet oldrs, ExpressionAnalysisResultSet temprs ) {
        assert oldrs.getId() != null;

        /*
         * Copy the results over.
         */
        Map<CompositeSequence, DifferentialExpressionAnalysisResult> p2der = new HashMap<CompositeSequence, DifferentialExpressionAnalysisResult>();

        for ( DifferentialExpressionAnalysisResult der : oldrs.getResults() ) {
            p2der.put( der.getProbe(), der );
        }

        Collection<DifferentialExpressionAnalysisResult> toAdd = new ArrayList<DifferentialExpressionAnalysisResult>();
        for ( DifferentialExpressionAnalysisResult newr : temprs.getResults() ) {
            if ( !p2der.containsKey( newr.getProbe() ) ) {
                toAdd.add( newr );

            }
            newr.setResultSet( oldrs );
        }

        if ( toAdd.isEmpty() ) {
            log.warn( "Somewhat surprisingly, no new results were added" );
        } else {
            log.info( toAdd.size() + " transient results added to the old analysis result set: " + oldrs.getId() );
        }

        boolean added = oldrs.getResults().addAll( toAdd );
        assert added;

        assert oldrs.getResults().size() >= toAdd.size();
    }

    /**
     * @param results
     * @param toUpdateResultSets
     */
    private void extendResultSets( Collection<DifferentialExpressionAnalysis> results,
            Collection<ExpressionAnalysisResultSet> toUpdateResultSets ) {
        for ( DifferentialExpressionAnalysis a : results ) {
            boolean found = false;
            // we should find a matching version for each resultset.

            for ( ExpressionAnalysisResultSet oldrs : toUpdateResultSets ) {

                assert oldrs.getId() != null;
                oldrs = this.differentialExpressionResultService.thaw( oldrs );

                for ( ExpressionAnalysisResultSet temprs : a.getResultSets() ) {
                    /*
                     * Compare the config
                     */
                    if ( configsAreEqual( temprs, oldrs ) ) {
                        found = true;

                        extendResultSet( oldrs, temprs );

                        break;
                    }
                }

                if ( !found )
                    throw new IllegalStateException( "Failed to find a matching existing result set for " + oldrs );
            }

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
     * @param diffExpressionAnalyses
     * @return
     */
    private Collection<DifferentialExpressionAnalysis> persistAnalyses( ExpressionExperiment expressionExperiment,
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses,
            DifferentialExpressionAnalysisConfig config ) {

        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
            DifferentialExpressionAnalysis persistentAnalysis = persistAnalysis( expressionExperiment, analysis, config );
            results.add( persistentAnalysis );
        }
        return results;
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
     * @param ee
     * @param copyMe
     * @param config
     * @return
     */
    private Collection<DifferentialExpressionAnalysis> redoWithoutSave( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe, DifferentialExpressionAnalysisConfig config ) {

        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();

        BioAssaySet experimentAnalyzed = copyMe.getExperimentAnalyzed();
        assert experimentAnalyzed != null;
        if ( experimentAnalyzed.equals( ee ) ) {
            results = analysisSelectionAndExecutionService.analyze( ee, config );
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet
                && ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment().equals( ee ) ) {
            DifferentialExpressionAnalysis subsetAnalysis = analysisSelectionAndExecutionService.analyze(
                    ( ExpressionExperimentSubSet ) experimentAnalyzed, config );

            results.add( subsetAnalysis );
        } else {
            throw new IllegalStateException(
                    "Cannot redo an analysis for one experiment if the analysis is for another (" + ee
                            + " is the proposed target, but analysis is from " + experimentAnalyzed );
        }

        return results;
    }

    /**
     * @param qvalueThreshold
     * @param results
     */
    private void removeUnwantedResults( Double qvalueThreshold, Collection<DifferentialExpressionAnalysisResult> results ) {

        if ( qvalueThreshold == null ) {
            log.info( "No qvalue threshold was set, retaining all " + results.size() + " results" );
            return;
        }
        Double workingThreshold = qvalueThreshold;

        int i = trimAboveThreshold( results, workingThreshold );

        /*
         * We want to have a minimum number so we always have something to look at.
         */
        if ( i < MINIMUM_NUMBER_OF_HITS_TO_SAVE && results.size() > MINIMUM_NUMBER_OF_HITS_TO_SAVE ) {
            List<DifferentialExpressionAnalysisResult> rl = new ArrayList<DifferentialExpressionAnalysisResult>(
                    results );
            Collections.sort( rl, new Comparator<DifferentialExpressionAnalysisResult>() {
                @Override
                public int compare( DifferentialExpressionAnalysisResult o1, DifferentialExpressionAnalysisResult o2 ) {
                    return o1.getPvalue().compareTo( o2.getPvalue() );
                }
            } );

            int indexOfLast = Math.min( results.size(), MINIMUM_NUMBER_OF_HITS_TO_SAVE ) - 1;
            workingThreshold = rl.get( indexOfLast ).getCorrectedPvalue();

            if ( workingThreshold == null || Double.isNaN( workingThreshold ) ) {
                throw new IllegalStateException( "Threshold was null or NaN" );
            }
            i = trimAboveThreshold( results, workingThreshold );
        }

        log.info( "Retained " + i + " results meeting qvalue of " + workingThreshold );

        /*
         * If we set a maximum value, it has to be some fraction of the total genes, at which point the results should
         * be discarded as too non-specific. We can't throw an exception, as there might be other factors in the same
         * analysis that are okay.
         */

    }

    /**
     * Print the distributions to a file.
     * 
     * @param extraSuffix eg qvalue, pvalue, score
     * @param histograms each column is a histogram
     * @param expressionExperiment
     * @param resulstsets in the same order as columns in the
     */
    private void saveDistributionMatrixToFile( DoubleMatrix<String, String> histograms,
            BioAssaySet expressionExperiment, List<ExpressionAnalysisResultSet> resultSetList ) {

        Long analysisId = resultSetList.iterator().next().getAnalysis().getId();

        assert analysisId != null;

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

        String histFileName = FileTools.cleanForFileName( shortName ) + ".an" + analysisId + "." + "pvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        File outputFile = new File( f, histFileName );

        /*
         * clobber, but usually won't since file names are per Analysis
         */
        try (FileWriter out = new FileWriter( outputFile, true );) {

            out.write( "# Gemma: differential expression statistics - " + "pvalues" + "\n" );
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

    /**
     * @param results
     * @param qvalueThreshold
     * @return
     */
    private int trimAboveThreshold( Collection<DifferentialExpressionAnalysisResult> results, Double qvalueThreshold ) {
        int i = 0;
        for ( Iterator<DifferentialExpressionAnalysisResult> it = results.iterator(); it.hasNext(); ) {
            DifferentialExpressionAnalysisResult r = it.next();

            if ( r.getPvalue() == null || Double.isNaN( r.getPvalue() ) || r.getCorrectedPvalue() == null
                    || r.getCorrectedPvalue() >= qvalueThreshold ) {
                it.remove();
            } else {
                i++;
            }

        }
        return i;
    }

    /**
     * Print the p-value and score distributions. Note that if there are multiple analyses for the experiment, each one
     * will get a set of files. The analysis must be 'thawed' already.
     * 
     * @param expressionExperiment
     * @param diffExpressionAnalysis - could be on a subset of the experiment.
     */
    private void writeDistributions( BioAssaySet expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {

        assert diffExpressionAnalysis.getId() != null;

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

        List<ExpressionAnalysisResultSet> resultSetList = new ArrayList<ExpressionAnalysisResultSet>();
        resultSetList.addAll( diffExpressionAnalysis.getResultSets() );

        List<String> factorNames = new ArrayList<String>();

        for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {

            String factorName = "";

            // these will be headings on the
            for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
                // Make a unique column heading.
                factorName = factorName + ( factorName.equals( "" ) ? "" : ":" ) + factor.getName()
                        + FACTOR_NAME_MANGLING_DELIMITER + factor.getId();
            }
            factorNames.add( factorName );

            Histogram pvalHist = addPvalueDistribution( resultSet );
            this.differentialExpressionResultService.update( resultSet );

            pvalueHistograms.add( pvalHist );

        }

        DoubleMatrix<String, String> pvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );

        fillDists( factorNames, pvalueHistograms, pvalueDists );

        saveDistributionMatrixToFile( pvalueDists, expressionExperiment, resultSetList );

        if ( timer.getTime() > 5000 ) {
            log.info( "Done writing distributions: " + timer.getTime() + "ms" );
        }
    }

}
