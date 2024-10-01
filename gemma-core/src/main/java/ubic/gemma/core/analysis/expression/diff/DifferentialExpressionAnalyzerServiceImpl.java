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
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.PvalueDistribution;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Differential expression service to run the differential expression analysis (and persist the results using the
 * appropriate data access objects).
 * Note that there is also a DifferentialExpressionAnalysisService (which handled CRUD for analyses). In contrast this
 * _does_ the analysis.
 *
 * @author keshav
 */
@Component
public class DifferentialExpressionAnalyzerServiceImpl implements DifferentialExpressionAnalyzerService {

    private static final Log log = LogFactory.getLog( DifferentialExpressionAnalyzerServiceImpl.class );
    @Autowired
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;
    @Autowired
    private AuditTrailService auditTrailService = null;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private DifferentialExpressionAnalysisHelperService helperService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Override
    public int deleteAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> diffAnalysis = differentialExpressionAnalysisService
                .findByExperiment( expressionExperiment );

        int result = 0;
        if ( diffAnalysis == null || diffAnalysis.isEmpty() ) {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .debug( "No differential expression analyses to remove for " + expressionExperiment
                            .getShortName() );
            return result;
        }

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .info( "Deleting old differential expression analysis for experiment " + expressionExperiment
                            .getShortName() + ": Analysis ID=" + de.getId() );
            differentialExpressionAnalysisService.remove( de );

            this.deleteStatistics( expressionExperiment, de );
            this.deleteAnalysisFiles( de );
            result++;
        }

        return result;
    }

    @Override
    public void deleteAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis existingAnalysis ) {
        DifferentialExpressionAnalyzerServiceImpl.log
                .info( "Deleting old differential expression analysis for experiment " + expressionExperiment
                        .getShortName() + " Analysis ID=" + existingAnalysis.getId() );
        differentialExpressionAnalysisService.remove( existingAnalysis );

        this.deleteStatistics( expressionExperiment, existingAnalysis );
        expressionDataFileService.deleteDiffExArchiveFile( existingAnalysis );
    }

    @Override
    public Collection<ExpressionAnalysisResultSet> extendAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis toUpdate ) {

        /*
         * One way to do this is redo without saving, and then copy the results over to the given result sets that
         * match. But that requires matching up old and new result sets.
         */
        toUpdate = differentialExpressionAnalysisService.thaw( toUpdate );
        DifferentialExpressionAnalysisConfig config = this.copyConfig( toUpdate );

        Collection<DifferentialExpressionAnalysis> results = this.redoWithoutSave( ee, toUpdate, config );

        /*
         * Match up old and new...
         */

        this.extendResultSets( results, toUpdate.getResultSets() );
        return toUpdate.getResultSets();

    }

    @Override
    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .getAnalyses( expressionExperiment );
        return differentialExpressionAnalysisService.thaw( expressionAnalyses );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe, boolean persist ) {

        if ( !differentialExpressionAnalysisService.canDelete( copyMe ) ) {
            throw new IllegalArgumentException(
                    "Cannot redo the analysis because it is included in a meta-analysis (or something). "
                            + "Delete the constraining entity first." );
        }

        copyMe = differentialExpressionAnalysisService.thaw( copyMe );

        DifferentialExpressionAnalyzerServiceImpl.log.info( "Will base analysis on old one: " + copyMe );
        DifferentialExpressionAnalysisConfig config = this.copyConfig( copyMe );
        boolean rnaSeq = this.expressionExperimentService.isRNASeq( ee );
        config.setUseWeights( rnaSeq );
        Collection<DifferentialExpressionAnalysis> results = this.redoWithoutSave( ee, copyMe, config );

        if ( persist ) {
            return this.persistAnalyses( ee, results, config );
        }
        return results;
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        try {
            // This might be redundant in some cases.
            boolean rnaSeq = this.expressionExperimentService.isRNASeq( expressionExperiment );
            config.setUseWeights( rnaSeq );

            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = analysisSelectionAndExecutionService
                    .analyze( expressionExperiment, config );

            if ( config.getPersist() ) {
                diffExpressionAnalyses = this.persistAnalyses( expressionExperiment, diffExpressionAnalyses, config );
            } else {
                DifferentialExpressionAnalyzerServiceImpl.log.info( "Will not persist results" );
            }

            return diffExpressionAnalyses;
        } catch ( Exception e ) {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .error( "Error during differential expression analysis: " + e.getMessage(), e );
            try {
                auditTrailService.addUpdateEvent( expressionExperiment,
                        FailedDifferentialExpressionAnalysisEvent.class,
                        ExceptionUtils.getStackTrace( e ) );
            } catch ( Exception e2 ) {
                DifferentialExpressionAnalyzerServiceImpl.log.error( "Could not attach failure audit event" );
            }
            throw new RuntimeException( e );
        }
    }

    /**
     * Made public for testing purposes only.
     *
     * @param config               config
     * @param analysis             analysis
     * @param expressionExperiment the experiment
     * @return DEA
     */
    @Override
    public DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis analysis, DifferentialExpressionAnalysisConfig config ) {

        this.deleteOldAnalyses( expressionExperiment, analysis, config.getFactorsToInclude() );
        StopWatch timer = new StopWatch();
        timer.start();

        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            // ensure that all result have the right resultSet
            for ( DifferentialExpressionAnalysisResult r : rs.getResults() ) {
                r.setResultSet( rs );
            }
            // add P-value distribution
            this.addPvalueDistribution( rs );
        }

        // now persist, everything is done by Hibernate in cascade
        DifferentialExpressionAnalyzerServiceImpl.log.info( "Saving results" );
        analysis = helperService.persistStub( analysis );

        log.info( "Done persisting, creating archive file" );

        // we do this here because now we have IDs for everything.
        if ( config.getMakeArchiveFile() ) {
            try {
                expressionDataFileService.writeDiffExArchiveFile( expressionExperiment, analysis, config );
            } catch ( IOException e ) {
                DifferentialExpressionAnalyzerServiceImpl.log
                        .error( "Unable to save the data to a file: " + e.getMessage() );
            }
        }

        // final transaction: audit.
        try {
            auditTrailService
                    .addUpdateEvent( expressionExperiment, DifferentialExpressionAnalysisEvent.class,
                            analysis.getDescription() + "; analysis id=" + analysis.getId() );
        } catch ( Exception e ) {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .error( "Error while trying to add audit event: " + e.getMessage(), e );
            DifferentialExpressionAnalyzerServiceImpl.log.error( "Continuing ..." );
            /*
             * We shouldn't fail completely due to this.
             */
        }

        if ( timer.getTime() > 5000 ) {
            DifferentialExpressionAnalyzerServiceImpl.log.info( "Save results: " + timer.getTime() + "ms" );
        }

        return analysis;

    }

    /**
     * Remove old files which will otherwise be cruft.
     *
     * @param ee       the experiment
     * @param analysis analysis
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void deleteStatistics( ExpressionExperiment ee, DifferentialExpressionAnalysis analysis ) {

        File f = this.prepareDirectoryForDistributions( ee );

        String histFileName =
                FileTools.cleanForFileName( ee.getShortName() ) + ".an" + analysis.getId() + "." + "pvalues"
                        + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        File oldf = new File( f, histFileName );
        if ( oldf.exists() && oldf.canWrite() ) {
            if ( !oldf.delete() ) {
                DifferentialExpressionAnalyzerServiceImpl.log.warn( "Could not remove: " + oldf );
            }
        }
    }

    private void addPvalueDistribution( ExpressionAnalysisResultSet resultSet ) {
        Histogram pvalHist = new Histogram( "", 100, 0.0, 1.0 );

        for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {

            Double pvalue = result.getPvalue();
            if ( pvalue != null )
                pvalHist.fill( pvalue );
        }

        PvalueDistribution pvd = PvalueDistribution.Factory.newInstance();
        pvd.setNumBins( 100 );
        pvd.setBinCounts( pvalHist.getArray() );
        resultSet.setPvalueDistribution( pvd ); // do not save yet.
    }

    private boolean configsAreEqual( ExpressionAnalysisResultSet temprs, ExpressionAnalysisResultSet oldrs ) {
        return temprs.getBaselineGroup().equals( oldrs.getBaselineGroup() )
                && temprs.getExperimentalFactors().size() == oldrs.getExperimentalFactors().size() && temprs
                .getExperimentalFactors().containsAll( oldrs.getExperimentalFactors() );
    }

    private DifferentialExpressionAnalysisConfig copyConfig( DifferentialExpressionAnalysis copyMe ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        if ( copyMe.getSubsetFactorValue() != null ) {
            config.setSubsetFactor( copyMe.getSubsetFactorValue().getExperimentalFactor() );
        }

        Collection<ExpressionAnalysisResultSet> resultSets = copyMe.getResultSets();
        Collection<ExperimentalFactor> factorsFromOldExp = new HashSet<>();
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<ExperimentalFactor> oldfactors = rs.getExperimentalFactors();
            factorsFromOldExp.addAll( oldfactors );

            /*
             * If we included the interaction before, include it again.
             */
            if ( oldfactors.size() == 2 ) {
                DifferentialExpressionAnalyzerServiceImpl.log.info( "Including interaction term" );
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
     */
    private void deleteAnalysisFiles( DifferentialExpressionAnalysis analysis ) {
        expressionDataFileService.deleteDiffExArchiveFile( analysis );
    }

    private void deleteOldAnalyses( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis newAnalysis, Collection<ExperimentalFactor> factors ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByExperiment( expressionExperiment );
        int numDeleted = 0;
        if ( diffAnalyses == null || diffAnalyses.isEmpty() ) {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .info( "No differential expression analyses to remove for " + expressionExperiment.getShortName() );
            return;
        }

        diffAnalyses = this.differentialExpressionAnalysisService.thaw( diffAnalyses );

        for ( DifferentialExpressionAnalysis existingAnalysis : diffAnalyses ) {

            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<>();

            for ( ExpressionAnalysisResultSet resultSet : existingAnalysis.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            FactorValue subsetFactorValueForExisting = existingAnalysis.getSubsetFactorValue();

            /*
             * Match if: factors are the same, and if this is a subset, it's the same subset factorvalue.
             */
            if ( factorsInAnalysis.size() == factors.size() && factorsInAnalysis.containsAll( factors ) && (
                    subsetFactorValueForExisting == null || subsetFactorValueForExisting
                            .equals( newAnalysis.getSubsetFactorValue() ) ) ) {

                DifferentialExpressionAnalyzerServiceImpl.log
                        .info( "Deleting analysis with ID=" + existingAnalysis.getId() );
                this.deleteAnalysis( expressionExperiment, existingAnalysis );

                numDeleted++;
            }
        }

        if ( numDeleted == 0 ) {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .info( "None of the other existing analyses were eligible for deletion" );
        }
    }

    private void extendResultSet( ExpressionAnalysisResultSet oldrs, ExpressionAnalysisResultSet temprs ) {
        assert oldrs.getId() != null;

        /*
         * Copy the results over.
         */
        Map<CompositeSequence, DifferentialExpressionAnalysisResult> p2der = new HashMap<>();

        for ( DifferentialExpressionAnalysisResult der : oldrs.getResults() ) {
            p2der.put( der.getProbe(), der );
        }

        Collection<DifferentialExpressionAnalysisResult> toAdd = new ArrayList<>();
        for ( DifferentialExpressionAnalysisResult newr : temprs.getResults() ) {
            if ( !p2der.containsKey( newr.getProbe() ) ) {
                toAdd.add( newr );

            }
            newr.setResultSet( oldrs );
        }

        if ( toAdd.isEmpty() ) {
            DifferentialExpressionAnalyzerServiceImpl.log.warn( "Somewhat surprisingly, no new results were added" );
        } else {
            DifferentialExpressionAnalyzerServiceImpl.log
                    .info( toAdd.size() + " transient results added to the old analysis result set: " + oldrs.getId() );
        }

        boolean added = oldrs.getResults().addAll( toAdd );
        assert added;

        assert oldrs.getResults().size() >= toAdd.size();
    }

    private void extendResultSets( Collection<DifferentialExpressionAnalysis> results,
            Collection<ExpressionAnalysisResultSet> toUpdateResultSets ) {
        for ( DifferentialExpressionAnalysis a : results ) {
            boolean found = false;
            // we should find a matching version for each resultset.

            for ( ExpressionAnalysisResultSet oldrs : toUpdateResultSets ) {

                assert oldrs.getId() != null;
                oldrs = expressionAnalysisResultSetService.thaw( oldrs );

                for ( ExpressionAnalysisResultSet temprs : a.getResultSets() ) {
                    /*
                     * Compare the config
                     */
                    if ( this.configsAreEqual( temprs, oldrs ) ) {
                        found = true;

                        this.extendResultSet( oldrs, temprs );

                        break;
                    }
                }

                if ( !found )
                    throw new IllegalStateException( "Failed to find a matching existing result set for " + oldrs );
            }

        }
    }

    private Collection<DifferentialExpressionAnalysis> persistAnalyses( ExpressionExperiment expressionExperiment,
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses,
            DifferentialExpressionAnalysisConfig config ) {

        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
            DifferentialExpressionAnalysis persistentAnalysis = this
                    .persistAnalysis( expressionExperiment, analysis, config );
            results.add( persistentAnalysis );
        }
        return results;
    }

    private File prepareDirectoryForDistributions( BioAssaySet expressionExperiment ) {
        if ( expressionExperiment instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet ss = ( ExpressionExperimentSubSet ) expressionExperiment;
            ExpressionExperiment source = ss.getSourceExperiment();

            File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory(
                    FileTools.cleanForFileName( source.getShortName() ) + ".Subset" + ss.getId() );
            FileTools.createDir( dir.toString() );
            return dir;
        } else if ( expressionExperiment instanceof ExpressionExperiment ) {
            File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory(
                    FileTools.cleanForFileName( ( ( ExpressionExperiment ) expressionExperiment ).getShortName() ) );
            FileTools.createDir( dir.toString() );
            return dir;
        } else {
            throw new IllegalStateException( "Cannot handle bioassay sets of type=" + expressionExperiment.getClass() );
        }

    }

    private Collection<DifferentialExpressionAnalysis> redoWithoutSave( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe, DifferentialExpressionAnalysisConfig config ) {

        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();

        BioAssaySet experimentAnalyzed = copyMe.getExperimentAnalyzed();
        assert experimentAnalyzed != null;
        if ( experimentAnalyzed.equals( ee ) ) {
            results = analysisSelectionAndExecutionService.analyze( ee, config );
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet
                && ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment().equals( ee ) ) {
            DifferentialExpressionAnalysis subsetAnalysis = analysisSelectionAndExecutionService
                    .analyze( ( ExpressionExperimentSubSet ) experimentAnalyzed, config );

            results.add( subsetAnalysis );
        } else {
            throw new IllegalStateException(
                    "Cannot redo an analysis for one experiment if the analysis is for another (" + ee
                            + " is the proposed target, but analysis is from " + experimentAnalyzed );
        }

        return results;
    }

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
     */
    public enum AnalysisType {
        GENERICLM, //
        OSTTEST, //one-sample
        OWA, //one-way ANOVA
        TTEST, //
        TWO_WAY_ANOVA_WITH_INTERACTION, //with interactions
        TWO_WAY_ANOVA_NO_INTERACTION //no interactions
    }

}
