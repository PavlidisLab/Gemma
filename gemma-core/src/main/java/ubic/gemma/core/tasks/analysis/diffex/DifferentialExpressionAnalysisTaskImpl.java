/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.tasks.analysis.diffex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentFactorUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;

/**
 * A differential expression analysis spaces task
 *
 * @author keshav
 */
@Component
@Scope("prototype")
public class DifferentialExpressionAnalysisTaskImpl
        extends AbstractTask<DifferentialExpressionAnalysisTaskCommand>
        implements DifferentialExpressionAnalysisTask {

    private static final Log log = LogFactory.getLog( DifferentialExpressionAnalysisTask.class.getName() );

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Override
    public TaskResult call() {

        if ( getTaskCommand() instanceof DifferentialExpressionAnalysisRemoveTaskCommand ) {
            DifferentialExpressionAnalysis toRemove = ( ( DifferentialExpressionAnalysisRemoveTaskCommand ) getTaskCommand() )
                    .getToRemove();

            if ( toRemove == null ) {
                throw new IllegalArgumentException( "Analysis to remove must not be null" );
            }

            log.info( "Removing analysis ..." );
            this.differentialExpressionAnalysisService.remove( toRemove );

            return newTaskResult( true );
        }

        Collection<DifferentialExpressionAnalysis> results = doAnalysis();

        HashSet<DifferentialExpressionAnalysis> minimalResults = new HashSet<>();
        for ( DifferentialExpressionAnalysis r : results ) {

            /* Don't send the full analysis to the space. Instead, create a minimal result. */
            DifferentialExpressionAnalysis minimalResult = DifferentialExpressionAnalysis.Factory.newInstance();
            minimalResult.setName( r.getName() );
            minimalResult.setDescription( r.getDescription() );
            minimalResults.add( minimalResult );
        }

        return newTaskResult( minimalResults );
    }

    private Collection<DifferentialExpressionAnalysis> doAnalysis() {
        ExpressionExperiment ee = getTaskCommand().getExpressionExperiment();
        ee = expressionExperimentService.thawLite( ee );

        if ( getTaskCommand().getToRedo() != null ) {
            log.info( "Redoing existing analysis" );
            return differentialExpressionAnalyzerService.redoAnalysis( ee, getTaskCommand().getToRedo() );
        }

        Collection<DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .getAnalyses( ee, true );

        if ( !diffAnalyses.isEmpty() ) {
            log.info(
                    "This experiment has some existing analyses; if they overlap with the new analysis they will be deleted after the run." );
        }

        Collection<DifferentialExpressionAnalysis> results;

        Collection<ExperimentalFactor> factors = getTaskCommand().getFactors();

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        boolean rnaSeq = expressionExperimentService.isRNASeq( ee );
        config.setUseWeights( rnaSeq );
        config.addFactorsToInclude( factors );
        config.setSubsetFactor( getTaskCommand().getSubsetFactor() );
        if ( getTaskCommand().isIncludeInteractions() && factors.size() == 2 ) {
            /*
             * We should not include 'batch' in an interaction. But I don't want to enforce that here.
             */
            for ( ExperimentalFactor ef : factors ) {
                if ( ExperimentFactorUtils.isBatchFactor( ef ) ) {
                    log.warn( "Batch is included in the interaction!" );
                }
            }
            config.addInteractionToInclude( factors ); // might get dropped.
        }

        AnalysisType analyzer = DiffExAnalyzerUtils.determineAnalysisType( ee, config );

        if ( analyzer == null ) {
            throw new IllegalStateException( "Dataset cannot be analyzed" );
        }

        config.setAnalysisType( analyzer );

        results = differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, config );

        return results;
    }
}
