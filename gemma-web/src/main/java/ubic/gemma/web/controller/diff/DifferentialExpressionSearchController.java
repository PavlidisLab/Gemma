/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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
package ubic.gemma.web.controller.diff;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionMetaAnalysisValueObject;
import ubic.gemma.core.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.tasks.visualization.DifferentialExpressionSearchTaskCommand;
import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.*;

/**
 * A controller used to get differential expression analysis and meta analysis results.
 *
 * @author keshav
 */
@Controller
public class DifferentialExpressionSearchController {

    private static final Log log = LogFactory.getLog( DifferentialExpressionSearchController.class.getName() );

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private GeneDifferentialExpressionService geneDifferentialExpressionService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private TaskRunningService taskRunningService;

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold ) {

        return this.getDifferentialExpression( geneId, threshold, null );
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold,
            Integer limit ) {

        Gene g = geneService.load( geneId );
        g = geneService.thaw( g );

        if ( g == null ) {
            return new ArrayList<>();
        }

        return geneDifferentialExpressionService.getDifferentialExpression( g, threshold, limit );
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned. This method is just like getDifferentialExpression but any analyses
     * with the 'batch' factor are filtered out because they are not biologically relevant
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpressionWithoutBatch( Long geneId,
            double threshold, Integer limit ) {

        Collection<DifferentialExpressionValueObject> analyses = getDifferentialExpression( geneId, threshold, limit );

        // for each DifferentialExpressionValueObject, check if its factors includes a batch factor and if so, remove
        // the batch factor
        Collection<DifferentialExpressionValueObject> toRemove = new ArrayList<>();
        for ( DifferentialExpressionValueObject analysis : analyses ) {
            for ( ExperimentalFactorValueObject factor : analysis.getExperimentalFactors() ) {
                if ( ExperimentalDesignUtils.isBatch( factor ) ) {
                    toRemove.add( analysis );
                }
            }
        }
        analyses.removeAll( toRemove );

        return analyses;
    }

    /**
     * AJAX entry.
     * Value objects returned contain experiments that have 2 factors and have had the diff analysis run on it.
     */
    public Collection<ExpressionExperimentExperimentalFactorValueObject> getFactors( final Collection<Long> eeIds ) {

        Collection<ExpressionExperimentExperimentalFactorValueObject> result = new HashSet<>();

        final Collection<Long> securityFilteredIds = securityFilterExpressionExperimentIds( eeIds );

        if ( securityFilteredIds.size() == 0 ) {
            return result;
        }

        log.debug( "Getting factors for experiments with ids: " + StringUtils
                .abbreviate( securityFilteredIds.toString(), 100 ) );

        Collection<Long> filteredEeIds = new HashSet<>();

        Map<Long, Collection<DifferentialExpressionAnalysis>> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigationIds( securityFilteredIds );

        if ( diffAnalyses.isEmpty() ) {
            log.debug( "No differential expression analyses for given ids: " + StringUtils.join( filteredEeIds, ',' ) );
            return result;
        }

        Collection<ExpressionExperimentValueObject> eevos = this.expressionExperimentService
                .loadValueObjects( diffAnalyses.keySet(), false );

        Map<Long, ExpressionExperimentValueObject> eevoMap = new HashMap<>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevoMap.put( eevo.getId(), eevo );
        }

        for ( Long id : diffAnalyses.keySet() ) {

            Collection<DifferentialExpressionAnalysis> analyses = diffAnalyses.get( id );

            for ( DifferentialExpressionAnalysis analysis : analyses ) {
                differentialExpressionAnalysisService.thaw( analysis );

                Collection<ExperimentalFactor> factors = new HashSet<>();
                for ( FactorAssociatedAnalysisResultSet fars : analysis.getResultSets() ) {
                    // FIXME includes factors making up interaction terms, but shouldn't
                    // matter, because they will be included as main effects too. If not, this will be wrong!
                    factors.addAll( fars.getExperimentalFactors() );
                }

                filteredEeIds.add( id );
                ExpressionExperimentValueObject eevo = eevoMap.get( id );
                ExpressionExperimentExperimentalFactorValueObject eeefvo = new ExpressionExperimentExperimentalFactorValueObject();
                eeefvo.setExpressionExperiment( eevo );
                eeefvo.setNumFactors( factors.size() );
                for ( ExperimentalFactor ef : factors ) {
                    ExperimentalFactorValueObject efvo = geneDifferentialExpressionService
                            .configExperimentalFactorValueObject( ef );
                    eeefvo.getExperimentalFactors().add( efvo );
                }

                result.add( eeefvo );
            }
        }
        log.info( "Filtered experiments.  Returning factors for experiments with ids: " + StringUtils
                .abbreviate( filteredEeIds.toString(), 100 ) );
        return result;
    }

    /**
     * AJAX - method used for main display metaheatmap.
     */
    @SuppressWarnings("unused") //used in js DiffExSearchAndVisualize
    public String scheduleDiffExpSearchTask( Long taxonId, ExpressionExperimentSetValueObject eevo,
            GeneSetValueObject gsvo ) {

        log.info( "Starting gene x condition search..." );
        // Load experiments
        Collection<ExpressionExperimentDetailsValueObject> experiments;
        List<String> datasetGroupNames = new ArrayList<>();
        if ( eevo.getExpressionExperimentIds().isEmpty() ) {
            if ( eevo.getId() != null ) {
                experiments = expressionExperimentSetService.getExperimentValueObjectsInSet( eevo.getId() );
            } else if ( eevo.getName() != null ) {
                Collection<ExpressionExperimentSet> eesets = expressionExperimentSetService
                        .findByName( eevo.getName() );
                if ( eesets.isEmpty() || eesets.size() > 1 ) {
                    throw new IllegalArgumentException( "Experiment set not found by name=" + eevo.getName() );
                }
                experiments = expressionExperimentSetService
                        .getExperimentValueObjectsInSet( eesets.iterator().next().getId() );

            } else {
                throw new IllegalArgumentException(
                        "Experiment group should either have an id or a list of ee ids, or a unique name" );
            }
        } else {
            experiments = loadExperimentsByIds( eevo.getExpressionExperimentIds() );
        }

        datasetGroupNames.add( eevo.getName() );

        // Load genes
        Collection<GeneValueObject> genes;
        if ( gsvo.getGeneIds().isEmpty() ) {
            genes = geneSetService.getGenesInGroup( gsvo );
        } else {
            genes = geneService.loadValueObjectsByIds( gsvo.getGeneIds() );
        }

        log.info( "Got genes" );
        // FIXME why not just pass in the eeset and geneset (security filtering could happen there)
        final DifferentialExpressionSearchTaskCommand taskCommand = new DifferentialExpressionSearchTaskCommand( genes,
                experiments, gsvo.getName(), eevo.getName() );

        String taskId = taskRunningService.submitLocalTask( taskCommand );

        log.info( "Scheduled search with task=" + taskId );

        return taskId;
    }

    /**
     * Returns the results of the meta-analysis.
     */
    private DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( Long geneId,
            Collection<DiffExpressionSelectedFactorCommand> selectedFactors, double threshold ) {

        Gene g = geneService.load( geneId );

        if ( g == null ) {
            log.warn( "No Gene with id=" + geneId );
            return null;
        }

        /* find experiments that have had the diff cli run on it and have the gene g (analyzed) - security filtered. */
        Collection<BioAssaySet> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        if ( experimentsAnalyzed.size() == 0 ) {
            throw new EntityNotFoundException( "No results were found: no experiment analyzed those genes" );
        }

        /* the 'chosen' factors (and their associated experiments) */
        Map<Long, Long> eeFactorsMap = new HashMap<>();
        for ( DiffExpressionSelectedFactorCommand selectedFactor : selectedFactors ) {
            Long eeId = selectedFactor.getEeId();
            eeFactorsMap.put( eeId, selectedFactor.getEfId() );
            if ( log.isDebugEnabled() )
                log.debug( eeId + " --> " + selectedFactor.getEfId() );
        }

        /*
         * filter experiments that had the diff cli run on it and are in the scope of eeFactorsMap eeIds
         * (active/available to the user).
         */
        Collection<BioAssaySet> activeExperiments;
        if ( eeFactorsMap.keySet() == null || eeFactorsMap.isEmpty() ) {
            activeExperiments = experimentsAnalyzed;
        } else {
            activeExperiments = new ArrayList<>();
            for ( BioAssaySet ee : experimentsAnalyzed ) {
                if ( eeFactorsMap.keySet().contains( ee.getId() ) ) {
                    activeExperiments.add( ee );
                }
            }
        }

        if ( activeExperiments.isEmpty() ) {
            throw new EntityNotFoundException(
                    "No results were found: none of the experiments selected analyzed those genes" );
        }

        return geneDifferentialExpressionService
                .getDifferentialExpressionMetaAnalysis( threshold, g, eeFactorsMap, activeExperiments );
    }

    private Collection<ExpressionExperimentDetailsValueObject> loadExperimentsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            throw new IllegalArgumentException( "No ids were provided" );
        }

        Collection<ExpressionExperimentDetailsValueObject> experiments = expressionExperimentService
                .loadDetailsValueObjects(null, false, ids, null, 0,0 );

        if ( experiments.isEmpty() ) {
            throw new EntityNotFoundException( "Could not access any experiments for " + ids.size() + " ids" );
        }

        return experiments;
    }

    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.load( ids );

        Collection<Long> filteredIds = new HashSet<>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }
}