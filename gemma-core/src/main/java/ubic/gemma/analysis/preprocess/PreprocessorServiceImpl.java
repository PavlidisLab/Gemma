/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Encapsulates steps that are done to expression data sets after they are loaded, or which can be triggered by certain
 * later events in the lifecycle of a data set. These include:
 * <ol>
 * <li>Deleting old analysis files and results, as these are invalidated by the subsequent steps.S
 * <li>Computing missing values - Basic processing
 * <li>Creation of "processed" vectors
 * <li>PCA
 * <li>Computing sample-wise correlation matrices for diagnostic plot
 * <li>Computing mean-variance data for diagnostic plots
 * </ol>
 * <p>
 * WORK IN PROGRESS
 * </p>
 * <p>
 * Other elements that can be considered
 * <ol>
 * <li>Reprocess from raw data? - not yet
 * <li>Switching to a merged array design and merging vectors
 * <li>(Re)normalization
 * <li>Getting information on batches
 * <li>Outlier detection
 * <li>Batch correction
 * <li>Ordering of vectors with respect to the experimental design? [probably not, this isn't a big problem]
 * <li>Populating the experimental design (guesses)?
 * <li>Autotagger .
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class PreprocessorServiceImpl implements PreprocessorService {

    private static Log log = LogFactory.getLog( PreprocessorServiceImpl.class );

    @Autowired
    private DifferentialExpressionAnalyzerService analyzerService;

    @Autowired
    private Probe2ProbeCoexpressionService coexpressionService;

    @Autowired
    private ExpressionDataFileService dataFileService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Autowired
    private SVDServiceHelper svdService;

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValueService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.PreprocessorService#process(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public void process( ExpressionExperiment ee ) throws PreprocessingException {

        ee = expressionExperimentService.thawLite( ee );

        try {
            removeInvalidatedData( ee );
            processForMissingValues( ee );
            processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

            // // refresh into context.
            ee = expressionExperimentService.thawLite( ee );

            processForSampleCorrelation( ee );
            // processForMeanVarianceRealtion(ee ); TODO

            processForPca( ee );

            /*
             * Redo old diff ex analyses
             */
            Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                    .findByInvestigation( ee );
            log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
            Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
            for ( DifferentialExpressionAnalysis copyMe : oldAnalyses ) {
                results.addAll( this.analyzerService.redoAnalysis( ee, copyMe ) );
            }

            // Alternatively, delete all the old analyses.
            // analyzerService.deleteAnalyses( ee );
        } catch ( Exception e ) {
            throw new PreprocessingException( e );
        }
    }

    /**
     * @param ee
     * @return
     */
    private boolean processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            log.warn( "Skipping postprocessing because experiment uses "
                    + "multiple platform types. Please check valid entry and run postprocessing separately." );
        }

        ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
        boolean wasProcessed = false;

        TechnologyType tt = arrayDesignUsed.getTechnologyType();
        if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
            log.info( ee + " uses a two-color array design, processing for missing values ..." );
            ee = expressionExperimentService.thawLite( ee );
            twoChannelMissingValueService.computeMissingValues( ee );
            wasProcessed = true;
        }

        return wasProcessed;
    }

    /**
     * @param ee
     */
    private void processForPca( ExpressionExperiment ee ) {
        svdService.svd( ee );
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     * 
     * @param ee
     */
    private void processForSampleCorrelation( ExpressionExperiment ee ) {
        sampleCoexpressionMatrixService.findOrCreate( ee );
    }

    /**
     * @param expExp
     */
    private void removeInvalidatedData( ExpressionExperiment expExp ) {
        coexpressionService.deleteLinks( expExp );

        try {
            dataFileService.deleteAllFiles( expExp );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.PreprocessorService#process(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, boolean)
     */
    @Override
    public void process( ExpressionExperiment ee, boolean light ) throws PreprocessingException {
        if ( light ) {
            try {
                removeInvalidatedData( ee );
                processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
                processForSampleCorrelation( ee );
                // processForMeanVarianceRealtion(ee ); TODO
                processForPca( ee );
                // analyzerService.deleteAnalyses( ee ); ??
            } catch ( Exception e ) {
                throw new PreprocessingException( e );
            }
        } else {
            process( ee );
        }

    }
}
