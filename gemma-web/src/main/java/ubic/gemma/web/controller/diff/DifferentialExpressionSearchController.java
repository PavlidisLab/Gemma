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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionMetaAnalysisValueObject;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.tasks.visualization.DifferentialExpressionSearchTaskCommand;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.*;

/**
 * A controller used to get differential expression analysis and meta analysis results.
 * 
 * @author keshav
 * @version $Id$ *
 */
@Controller
public class DifferentialExpressionSearchController {

    private static Log log = LogFactory.getLog( DifferentialExpressionSearchController.class.getName() );

    private static final int MAX_GENES_PER_QUERY = 20;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private GeneDifferentialExpressionService geneDifferentialExpressionService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private TaskRunningService taskRunningService;

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold ) {

        return this.getDifferentialExpression( geneId, threshold, null );
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold,
            Integer limit ) {

        Gene g = geneService.thaw( geneService.load( geneId ) );

        if ( g == null ) {
            return new ArrayList<DifferentialExpressionValueObject>();
        }

        return geneDifferentialExpressionService.getDifferentialExpression( g, threshold, limit );
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned. This method is just like getDifferentialExpression but any analyses
     * with the 'batch' factor are filtered out because they are not biologically relevant
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpressionWithoutBatch( Long geneId,
            double threshold, Integer limit ) {

        Collection<DifferentialExpressionValueObject> analyses = getDifferentialExpression( geneId, threshold, limit );

        // for each DifferentialExpressionValueObject, check if its factors includes a batch factor and if so, remove
        // the batch factor
        Collection<DifferentialExpressionValueObject> toRemove = new ArrayList<DifferentialExpressionValueObject>();
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
     * AJAX entry. Returns the meta-analysis results.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return <p>
     *         FIXME is this actually used?
     */
    public Collection<DifferentialExpressionMetaAnalysisValueObject> getDiffExpressionForGenes(
            DiffExpressionSearchCommand command ) {

        Collection<Long> eeScopeIds = command.getEeIds();
        int eeScopeSize = 0;

        if ( eeScopeIds != null && !eeScopeIds.isEmpty() ) {

            // do we need to validate these ids further? It should get checked late (in the analysis stage)

            eeScopeSize = eeScopeIds.size();
        } else {
            if ( command.getEeSetName() != null ) {
                Collection<ExpressionExperimentSet> eeSet = this.expressionExperimentSetService.findByName( command
                        .getEeSetName() );

                if ( eeSet == null || eeSet.isEmpty() ) {
                    throw new IllegalArgumentException( "Unknown or ambiguous set name: " + command.getEeSetName() );
                }

                eeScopeSize = eeSet.iterator().next().getExperiments().size();
            } else {
                Long eeSetId = command.getEeSetId();
                if ( eeSetId >= 0 ) {
                    ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
                    // validation/security check.
                    if ( eeSet == null ) {
                        throw new IllegalArgumentException( "No such set with id=" + eeSetId );
                    }
                    eeScopeSize = eeSet.getExperiments().size();
                }
            }

        }

        Collection<Long> geneIds = command.getGeneIds();

        if ( geneIds.size() > MAX_GENES_PER_QUERY ) {
            throw new IllegalArgumentException( "Too many genes selected, please limit searches to "
                    + MAX_GENES_PER_QUERY );
        }

        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = command.getSelectedFactors();

        double threshold = command.getThreshold();

        Collection<DifferentialExpressionMetaAnalysisValueObject> mavos = new ArrayList<DifferentialExpressionMetaAnalysisValueObject>();
        for ( long geneId : geneIds ) {
            DifferentialExpressionMetaAnalysisValueObject mavo = getDifferentialExpressionMetaAnalysis( geneId,
                    selectedFactors, threshold );

            if ( mavo == null ) {
                continue; // no results.
            }

            mavo.setSortKey();
            if ( selectedFactors != null && !selectedFactors.isEmpty() ) {
                mavo.setNumSearchedExperiments( selectedFactors.size() );
            }

            mavo.setNumExperimentsInScope( eeScopeSize );

            mavos.add( mavo );

        }

        return mavos;
    }

    /**
     * AJAX entry.
     * <p>
     * Value objects returned contain experiments that have 2 factors and have had the diff analysis run on it.
     * <p>
     * FIXME Is this actually used?
     * 
     * @param eeIds
     */
    public Collection<ExpressionExperimentExperimentalFactorValueObject> getFactors( final Collection<Long> eeIds ) {

        Collection<ExpressionExperimentExperimentalFactorValueObject> result = new HashSet<ExpressionExperimentExperimentalFactorValueObject>();

        final Collection<Long> securityFilteredIds = securityFilterExpressionExperimentIds( eeIds );

        if ( securityFilteredIds.size() == 0 ) {
            return result;
        }

        log.debug( "Getting factors for experiments with ids: "
                + StringUtils.abbreviate( securityFilteredIds.toString(), 100 ) );

        Collection<Long> filteredEeIds = new HashSet<Long>();

        Map<Long, Collection<DifferentialExpressionAnalysis>> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigationIds( securityFilteredIds );

        if ( diffAnalyses.isEmpty() ) {
            log.debug( "No differential expression analyses for given ids: " + StringUtils.join( filteredEeIds, ',' ) );
            return result;
        }

        Collection<ExpressionExperimentValueObject> eevos = this.expressionExperimentService.loadValueObjects(
                diffAnalyses.keySet(), false );

        Map<Long, ExpressionExperimentValueObject> eevoMap = new HashMap<Long, ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevoMap.put( eevo.getId(), eevo );
        }

        for ( Long id : diffAnalyses.keySet() ) {

            Collection<DifferentialExpressionAnalysis> analyses = diffAnalyses.get( id );

            for ( DifferentialExpressionAnalysis analysis : analyses ) {
                differentialExpressionAnalysisService.thaw( analysis );

                Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
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
        log.info( "Filtered experiments.  Returning factors for experiments with ids: "
                + StringUtils.abbreviate( filteredEeIds.toString(), 100 ) );
        return result;
    }

    /**
     * AJAX - method used for main display metaheatmap.
     * 
     * @param taxonId
     * @param datasetValueObjects
     * @param geneValueObjects
     * @param geneSessionGroupQueries
     * @param experimentSessionGroupQueries
     * @return
     */
    public String scheduleDiffExpSearchTask( Long taxonId,
            Collection<ExpressionExperimentSetValueObject> datasetValueObjects,
            Collection<GeneSetValueObject> geneValueObjects, List<String> geneSessionGroupQueries,
            List<String> experimentSessionGroupQueries ) {

        log.info( "Starting gene x condition search..." );
        // Load experiments
        List<Collection<ExpressionExperiment>> experiments = new ArrayList<Collection<ExpressionExperiment>>();
        List<String> datasetGroupNames = new ArrayList<String>();
        for ( ExpressionExperimentSetValueObject eevo : datasetValueObjects ) {
            if ( eevo != null ) {
                // fixme temporary workaroud.
                if ( eevo.getExpressionExperimentIds().isEmpty() ) {
                    if ( eevo.getId() != null ) {
                        experiments.add( expressionExperimentSetService.getExperimentsInSet( eevo.getId() ) );
                    } else {
                        // session-bound.
                        experiments.add( loadExperimentsByIds( eevo.getExpressionExperimentIds() ) );
                    }
                } else {
                    experiments.add( expressionExperimentService.loadMultiple( eevo.getExpressionExperimentIds() ) );
                }
                datasetGroupNames.add( eevo.getName() );
            }
        }

        log.info( "Got experiments for set" );

        // Load genes
        List<List<Gene>> genes = new ArrayList<List<Gene>>();
        List<String> geneGroupNames = new ArrayList<String>();

        for ( GeneSetValueObject gsvo : geneValueObjects ) {
            if ( gsvo != null ) {
                geneGroupNames.add( gsvo.getName() );
                genes.add( new ArrayList<Gene>( geneService.loadMultiple( gsvo.getGeneIds() ) ) );
            }
        }

        log.info( "Got genes" );

        final DifferentialExpressionSearchTaskCommand taskCommand = new DifferentialExpressionSearchTaskCommand( genes,
                experiments, geneGroupNames, datasetGroupNames );

        String taskId = taskRunningService.submitLocalTask( taskCommand );

        log.info( "Scheduled search with task=" + taskId );

        return taskId;
    }

    /**
     * Returns the results of the meta-analysis.
     * 
     * @param geneId
     * @param selectedFactors
     * @param threshold
     * @return
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
        Map<Long, Long> eeFactorsMap = new HashMap<Long, Long>();
        for ( DiffExpressionSelectedFactorCommand selectedFactor : selectedFactors ) {
            Long eeId = selectedFactor.getEeId();
            eeFactorsMap.put( eeId, selectedFactor.getEfId() );
            if ( log.isDebugEnabled() ) log.debug( eeId + " --> " + selectedFactor.getEfId() );
        }

        /*
         * filter experiments that had the diff cli run on it and are in the scope of eeFactorsMap eeIds
         * (active/available to the user).
         */
        Collection<BioAssaySet> activeExperiments = null;
        if ( eeFactorsMap.keySet() == null || eeFactorsMap.isEmpty() ) {
            activeExperiments = experimentsAnalyzed;
        } else {
            activeExperiments = new ArrayList<BioAssaySet>();
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

        DifferentialExpressionMetaAnalysisValueObject mavo = geneDifferentialExpressionService
                .getDifferentialExpressionMetaAnalysis( threshold, g, eeFactorsMap, activeExperiments );

        return mavo;
    }

    /**
     * @param ids
     * @return
     */
    private Collection<ExpressionExperiment> loadExperimentsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            throw new IllegalArgumentException( "No ids were provided" );
        }

        Collection<ExpressionExperiment> experiments = expressionExperimentService.loadMultiple( ids );

        if ( experiments.isEmpty() ) {
            throw new EntityNotFoundException( "Could not access any experiments for " + ids.size() + " ids" );
        }

        return experiments;
    }

    /**
     * @param ids
     * @return
     */
    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.loadMultiple( ids );

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }
}