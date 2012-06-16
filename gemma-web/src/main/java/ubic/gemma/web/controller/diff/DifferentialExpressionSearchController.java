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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchService;
import ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject;

/**
 * A controller used to get differential expression analysis and meta analysis results.
 * 
 * @author keshav
 * @version $Id$ *
 */
@Controller
public class DifferentialExpressionSearchController {

    private static Log log = LogFactory.getLog( DifferentialExpressionSearchController.class.getName() );

    // private static final double DEFAULT_THRESHOLD = 0.01;

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
    private DifferentialExpressionGeneConditionSearchService geneConditionSearchService;

    /**
     * @param taskId
     * @return
     */
    public DifferentialExpressionGenesConditionsValueObject getDiffExpSearchResult( String taskId ) {
        return this.geneConditionSearchService.getDiffExpSearchResult( taskId );
    }

    /**
     * @param taskId
     * @return
     */
    public ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchServiceImpl.TaskProgress getDiffExpSearchTaskProgress(
            String taskId ) {
        return this.geneConditionSearchService.getDiffExpSearchTaskProgress( taskId );
    }

    /**
     * AJAX
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
                experiments.add( loadExperimentsByIds( eevo.getExpressionExperimentIds() ) );
                datasetGroupNames.add( eevo.getName() );
            }
        }

        // Load genes
        List<List<Gene>> genes = new ArrayList<List<Gene>>();
        List<String> geneGroupNames = new ArrayList<String>();

        for ( GeneSetValueObject gsvo : geneValueObjects ) {
            if ( gsvo != null ) {
                geneGroupNames.add( gsvo.getName() );
                genes.add( new ArrayList<Gene>( geneService.loadMultiple( gsvo.getGeneIds() ) ) );
            }
        }

        String taskId = geneConditionSearchService.scheduleDiffExpSearchTask( genes, experiments, geneGroupNames,
                datasetGroupNames );
        return taskId;
    }

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
     * AJAX entry which returns differential expression results for the gene with the given id, in the selected factors,
     * at the given significance threshold.
     * 
     * @param geneId
     * @param threshold corrected pvalue threshold (normally this means FDR)
     * @param factorMap
     * @deprecated as far as I can tell this is not used.
     * @return
     */
    @Deprecated
    public Collection<DifferentialExpressionValueObject> getDifferentialExpressionForFactors( Long geneId,
            double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( factorMap.isEmpty() || geneId == null ) {
            return null;
        }

        Gene g = geneService.load( geneId );
        Collection<DifferentialExpressionValueObject> result = geneDifferentialExpressionService
                .getDifferentialExpression( g, threshold, factorMap );

        return result;
    }

    /**
     * AJAX entry. Returns the meta-analysis results.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return
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

    public ExpressionExperimentSetService getExpressionExperimentSetService() {
        return expressionExperimentSetService;
    }

    /**
     * AJAX entry.
     * <p>
     * Value objects returned contain experiments that have 2 factors and have had the diff analysis run on it.
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
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    /**
     * @param geneDifferentialExpressionService
     */
    public void setGeneDifferentialExpressionService(
            GeneDifferentialExpressionService geneDifferentialExpressionService ) {
        this.geneDifferentialExpressionService = geneDifferentialExpressionService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    // /**
    // * @param fs
    // * @return
    // */
    // private Collection<DiffExpressionSelectedFactorCommand> extractFactorInfo( String fs ) {
    // Collection<DiffExpressionSelectedFactorCommand> selectedFactors = new
    // HashSet<DiffExpressionSelectedFactorCommand>();
    // try {
    // if ( fs != null ) {
    // String[] fss = fs.split( "," );
    // for ( String fm : fss ) {
    // String[] m = fm.split( "\\." );
    // if ( m.length != 2 ) {
    // continue;
    // }
    // String eeIdStr = m[0];
    // String efIdStr = m[1];
    //
    // Long eeId = Long.parseLong( eeIdStr );
    // Long efId = Long.parseLong( efIdStr );
    // DiffExpressionSelectedFactorCommand dsfc = new DiffExpressionSelectedFactorCommand( eeId, efId );
    // selectedFactors.add( dsfc );
    // }
    // }
    // } catch ( NumberFormatException e ) {
    // log.warn( "Error parsing factor info" );
    // }
    // return selectedFactors;
    // }

    /**
     * Returns the results of the meta-analysis.
     * 
     * @param geneId
     * @param eeIds
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

    // /*
    // * Helper method to get factor values. TODO: Fix FactoValue class to return correct factor value in the first
    // place.
    // */
    // private String getFactorValueString( FactorValue fv ) {
    // if ( fv == null ) return "null";
    //
    // if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
    // String fvString = "";
    // for ( Characteristic c : fv.getCharacteristics() ) {
    // fvString += c.getValue() + " ";
    // }
    // return fvString;
    // } else if ( fv.getMeasurement() != null ) {
    // return fv.getMeasurement().getValue();
    // } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
    // return fv.getValue();
    // } else
    // return "absent ";
    // }

    /**
     * @param ids
     * @return
     */
    private Collection<ExpressionExperiment> loadExperimentsByIds( Collection<Long> ids ) {
        Collection<ExpressionExperiment> experiments = expressionExperimentService.loadMultiple( ids );

        if ( experiments.isEmpty() ) {
            throw new EntityNotFoundException( "Could not access any experiments." );
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

    // TODO: Dead code?
    // /*
    // * Handles the case exporting results as text.
    // *
    // * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
    // * HttpServletRequest, javax.servlet.http.HttpServletResponse)
    // */
    // @Override
    // protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
    // throws Exception {
    //
    // if ( request.getParameter( "export" ) == null ) return new ModelAndView( this.getFormView() );
    //
    // // -------------------------
    // // Download diff expression data for a specific diff expresion search
    //
    // double threshold = DEFAULT_THRESHOLD;
    // try {
    // threshold = Double.parseDouble( request.getParameter( "t" ) );
    // } catch ( NumberFormatException e ) {
    // log.warn( "invalid threshold; using default " + threshold );
    // }
    //
    // Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
    //
    // Long eeSetId = null;
    // Collection<Long> eeIds = null;
    // try {
    // eeSetId = Long.parseLong( request.getParameter( "a" ) );
    // } catch ( NumberFormatException e ) {
    // //
    // }
    // if ( eeSetId == null ) {
    // eeIds = extractIds( request.getParameter( "ees" ) );
    // }
    //
    // String fs = request.getParameter( "fm" );
    // Collection<DiffExpressionSelectedFactorCommand> selectedFactors = extractFactorInfo( fs );
    //
    // DiffExpressionSearchCommand command = new DiffExpressionSearchCommand();
    // command.setGeneIds( geneIds );
    // command.setEeSetId( eeSetId );
    // command.setEeIds( eeIds );
    // command.setSelectedFactors( selectedFactors );
    // command.setThreshold( threshold );
    //
    // Collection<DifferentialExpressionMetaAnalysisValueObject> result = getDiffExpressionForGenes( command );
    //
    // ModelAndView mav = new ModelAndView( new TextView() );
    //
    // StringBuilder buf = new StringBuilder();
    //
    // for ( DifferentialExpressionMetaAnalysisValueObject demavo : result ) {
    // buf.append( demavo );
    // }
    //
    // String output = buf.toString();
    //
    // mav.addObject( "text", output.length() > 0 ? output : "no results" );
    // return mav;
    //
    // }
    //
    //
    // /**
    // * Returns a collection of {@link Long} ids from strings.
    // *
    // * @param idString
    // * @return
    // */
    // protected Collection<Long> extractIds( String idString ) {
    // Collection<Long> ids = new ArrayList<Long>();
    // if ( idString != null ) {
    // for ( String s : idString.split( "," ) ) {
    // try {
    // ids.add( Long.parseLong( s.trim() ) );
    // } catch ( NumberFormatException e ) {
    // log.warn( "invalid id " + s );
    // }
    // }
    // }
    // return ids;
    // }

}