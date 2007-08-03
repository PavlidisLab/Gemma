/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.coexpression.CommonCoexpressionValueObject;
import ubic.gemma.model.coexpression.MultipleCoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.MultipleCoexpressionTypeValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormBindController;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper;
import ubic.gemma.web.util.ConfigurationCookie;
import ubic.gemma.web.util.MessageUtil;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link stringency} sets the number of data sets the link must be seen in before it is listed in the results, and
 * {@link species} sets the type of species to search. {@link keywords} restrict the search.
 * 
 * @author luke
 * @version $Id$
 * @spring.bean id="multipleCoexpressionSearchController"
 * @spring.property name = "commandName" value="multipleCoexpressionSearchCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.coexpressionSearch.MultipleCoexpressionSearchCommand"
 * @spring.property name = "formView" value="searchCoexpressionMultiple"
 * @spring.property name = "successView" value="searchCoexpressionMultiple"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "taxonService" ref="taxonService"
 * @spring.property name = "searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "geneOntologyService" ref="geneOntologyService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class MultipleCoexpressionSearchController extends BackgroundProcessingFormBindController {
    private static Log log = LogFactory.getLog( MultipleCoexpressionSearchController.class.getName() );

    private int MAX_GENES_TO_RETURN = 50;
    private int DEFAULT_STRINGENCY = 3;
    private int DEFAULT_COMMONALITY = 2;

    private static final String COOKIE_NAME = "multipleCoexpressionSearchCookie";
    private static final int MAX_OVERLAP = 40;

    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private SearchService searchService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private GeneOntologyService geneOntologyService;

    public MultipleCoexpressionSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        MultipleCoexpressionSearchCommand csc = new MultipleCoexpressionSearchCommand();

        if ( request.getParameter( "searchString" ) != null ) {
            loadGETParameters( request, csc );
        } else {
            loadCookie( request, csc );
        }
        return csc;

    }

    private Collection<Gene> findGenes( MultipleCoexpressionSearchCommand commandObject, String searchString ) {

        Collection<Gene> genesFound = new HashSet<Gene>();

        // // find the genes specified by the search
        // // if an id is specified, find the identified gene
        // // if there is no exact search specified, do an inexact search
        // // if exact search is on, find only by official symbol
        // // if exact search is auto (usually from the front page), check if there is an exact search match. If there
        // is
        // // none, do inexact search.
        // if ( commandObject.getId() != null ) {
        // Gene g = geneService.load( Long.parseLong( commandObject.getId() ) );
        // genesFound.add( g );
        // commandObject.setExactSearch( g.getOfficialName() );
        // } else if ( commandObject.getExactSearch() == null ) {
        // genesFound.addAll( searchService.geneDbSearch( commandObject.getSearchString() ) );
        // genesFound.addAll( searchService.compassGeneSearch( commandObject.getSearchString() ) );
        // } else if ( commandObject.getGeneIdSearch().equalsIgnoreCase( "true" ) ) {
        // String geneId = commandObject.getSearchString();
        // Long id = Long.parseLong( geneId );
        // Collection<Long> ids = new ArrayList<Long>();
        // ids.add( id );
        // genesFound.addAll( geneService.load( ids ) );
        // } else if ( commandObject.getExactSearch().equalsIgnoreCase( "on" ) ) {
        // genesFound.addAll( geneService.findByOfficialSymbol( commandObject.getSearchString() ) );
        // } else {
        // genesFound.addAll( geneService.findByOfficialSymbol( commandObject.getSearchString() ) );
        // if ( genesFound.size() == 0 ) {
        // genesFound.addAll( searchService.geneDbSearch( commandObject.getSearchString() ) );
        // genesFound.addAll( searchService.compassGeneSearch( commandObject.getSearchString() ) );
        // }
        // }

        genesFound.addAll( geneService.findByOfficialSymbol( searchString ) );

        // filter genes by Taxon

        Collection<Gene> genesToRemove = new ArrayList<Gene>();
        Taxon taxon = commandObject.getTaxon();
        if ( taxon != null && taxon.getId() != null ) {
            for ( Gene gene : genesFound ) {
                if ( gene.getTaxon().getId().longValue() != taxon.getId().longValue() ) {
                    genesToRemove.add( gene );
                }
            }
            genesFound.removeAll( genesToRemove );
        }

        return genesFound;
    }

    /**
     * Mock function - do not use.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings( { "unused", "unchecked" })
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        MultipleCoexpressionSearchCommand commandObject = ( ( MultipleCoexpressionSearchCommand ) command );

        // try to process the geneListString; if any of the entries don't match a single gene, have the
        // user correct it;
        BufferedReader reader = new BufferedReader( new StringReader( commandObject.getGeneListString() ) );
        Collection<Gene> queryGenes = new HashSet<Gene>();
        try {
            String geneName;
            while ( ( geneName = reader.readLine() ) != null ) {
                Collection<Gene> genesFound = findGenes( commandObject, geneName );
                if ( genesFound.isEmpty() ) {
                    saveMessage( request, "No genes matched '" + geneName + "'. Please correct that term." );
                    return super.showForm( request, response, errors );
                }
                if ( genesFound.size() > 1 ) {
                    saveMessage( request, "Multiple genes matched '" + geneName + "'. Please refine that term." );
                    return super.showForm( request, response, errors );
                } else {
                    queryGenes.add( ( Gene ) ( genesFound.toArray()[0] ) );
                }
            }
        } catch ( IOException e ) {
            // this will never happen because StringReader.read() doesn't actually throw an IOException, but...
            saveMessage( request, "There was an error processing the list of genes." );
            return super.showForm( request, response, errors );
        }

        // if we've made it here, genesFound contains a list of uniquely specified query genes...
        // TODO should we check to see if they've asked for too many?
        commandObject.setSourceGenes( queryGenes );

        // find coexpressed genes

        // find expressionExperiments via lucene if the query is eestring-constrained
        Collection<ExpressionExperiment> ees;
        if ( StringUtils.isNotBlank( commandObject.getEeSearchString() ) ) {
            ees = searchService.compassExpressionSearch( commandObject.getEeSearchString() );
            if ( ees.size() == 0 ) {
                saveMessage( request, "No datasets matched - defaulting to all datasets" );
            }
        } else {
            ees = new ArrayList<ExpressionExperiment>();
        }

        Integer numExpressionExperiments = 0;

        // iterate over the genes and find the list of expression experiments
        Taxon taxon = commandObject.getTaxon();
        Collection<Long> possibleEEs = new HashSet<Long>();
        for ( Gene gene : commandObject.getSourceGenes() ) {
            possibleEEs.addAll( expressionExperimentService.findByGene( gene ) );
        }

        if ( possibleEEs.isEmpty() ) {
            ModelAndView mav = super.showForm( request, errors, getFormView() );
            saveMessage( request, "There are no " + taxon.getScientificName()
                    + " arrays in the system that assay for any of the genes in the list" );
            return mav;
        }

        if ( ees.size() > 0 ) {
            // if there are matches, fihter the expression experiments first by taxon

            Collection<ExpressionExperiment> eeToRemove = new HashSet<ExpressionExperiment>();
            for ( ExpressionExperiment ee : ees ) {
                Taxon t = expressionExperimentService.getTaxon( ee.getId() );
                if ( t.getId().longValue() != taxon.getId().longValue() ) eeToRemove.add( ee );

                if ( !possibleEEs.contains( ee.getId() ) ) eeToRemove.add( ee );
            }
            ees.removeAll( eeToRemove );

            if ( ees.isEmpty() ) {
                ModelAndView mav = super.showForm( request, errors, getFormView() );
                saveMessage( request, "There are no " + taxon.getScientificName()
                        + " arrays in the system that assay for any of the genes in the list matching search criteria "
                        + commandObject.getEeSearchString() );
                return mav;
            }

        } else {
            ees = expressionExperimentService.loadMultiple( possibleEEs );
        }
        commandObject.setToUseEE( ees );
        numExpressionExperiments = ees.size();

        // stringency. Cannot be less than 1; set to one if it is
        Integer stringency = commandObject.getStringency();
        if ( stringency == null ) {
            stringency = DEFAULT_STRINGENCY;
        } else if ( stringency < 1 ) {
            stringency = DEFAULT_STRINGENCY;
        }
        commandObject.setStringency( stringency );

        // TODO validate the minimum number of common query genes here

        // ===================================================================================
        // Taking out progress for the mean time while we figure out the back button problem
        // return startJob( command, request, response, errors );
        // ===================================================================================
        StopWatch watch = new StopWatch();
        watch.start();

        MultipleCoexpressionCollectionValueObject coexpressions = ( MultipleCoexpressionCollectionValueObject ) geneService
                .getMultipleCoexpressionResults( commandObject.getSourceGenes(), commandObject.getToUseEE(),
                        commandObject.getStringency() );

        // get all the coexpressed genes and sort them by dataset count
        List<CommonCoexpressionValueObject> coexpressedGenes = new ArrayList<CommonCoexpressionValueObject>();
        coexpressedGenes.addAll( coexpressions.getCommonCoexpressedGenes() );
        // sort coexpressed genes by dataset count
        Collections.sort( coexpressedGenes, new CommonCoexpressionComparator() );

        // get all the coexpressed predicted genes and sort them by dataset count
        List<CommonCoexpressionValueObject> coexpressedPredictedGenes = new ArrayList<CommonCoexpressionValueObject>();
        coexpressedPredictedGenes.addAll( coexpressions.getCommonCoexpressedPredictedGenes() );
        // sort coexpressed genes by dataset count
        Collections.sort( coexpressedPredictedGenes, new CommonCoexpressionComparator() );

        // get all the coexpressed probe aligned regions and sort them by dataset count
        List<CommonCoexpressionValueObject> coexpressedAlignedRegions = new ArrayList<CommonCoexpressionValueObject>();
        coexpressedAlignedRegions.addAll( coexpressions.getCommonCoexpressedProbeAlignedRegions() );
        // sort coexpressed genes by dataset count
        Collections.sort( coexpressedAlignedRegions, new CommonCoexpressionComparator() );

        Collection<ExpressionExperimentValueObject> geneEEVos = retreiveEEFromDB( coexpressions
                .getGeneCoexpressionType().getExpressionExperimentIds(), coexpressions.getGeneCoexpressionType() );
        Collection<ExpressionExperimentValueObject> predictedEEVos = retreiveEEFromDB( coexpressions
                .getPredictedCoexpressionType().getExpressionExperimentIds(), coexpressions
                .getPredictedCoexpressionType() );
        Collection<ExpressionExperimentValueObject> alignedEEVos = retreiveEEFromDB( coexpressions
                .getProbeAlignedCoexpressionType().getExpressionExperimentIds(), coexpressions
                .getProbeAlignedCoexpressionType() );

        // Sort the Expression Experiments by contributing links.

        if ( coexpressedGenes.size() == 0 ) {
            this.saveMessage( request, "No genes are coexpressed with the given stringency." );
        }

        Cookie cookie = new MultipleCoexpressionSearchCookie( commandObject );
        response.addCookie( cookie );

        // Long numPositiveCoexpressedGenes = new Long( coexpressions.getGeneCoexpressionType()
        // .getPositiveStringencyLinkCount() );
        // Long numNegativeCoexpressedGenes = new Long( coexpressions.getGeneCoexpressionType()
        // .getNegativeStringencyLinkCount() );
        Integer minimumCommonQueryGenes = new Integer( coexpressions.getMinimumCommonQueries() );
        Long numGenes = new Long( coexpressions.getNumGenes() );
        Long numPredictedGenes = new Long( coexpressions.getNumPredictedGenes() );
        Long numProbeAlignedRegions = new Long( coexpressions.getNumProbeAlignedRegions() );
        Long numStringencyGenes = new Long( coexpressedGenes.size() );
        Long numStringencyPredictedGenes = new Long( coexpressedPredictedGenes.size() );
        Long numStringencyProbeAlignedRegions = new Long( coexpressedAlignedRegions.size() );
        String sourceGenesDescription = buildSourceGenesDescription( commandObject.getSourceGenes() );

        ModelAndView mav = super.showForm( request, response, errors );

        mav.addObject( "coexpressedGenes", coexpressedGenes );
        mav.addObject( "coexpressedPredictedGenes", coexpressedPredictedGenes );
        mav.addObject( "coexpressedAlignedRegions", coexpressedAlignedRegions );

        mav.addObject( "expressionExperiments", geneEEVos );
        mav.addObject( "predictedExpressionExperiments", predictedEEVos );
        mav.addObject( "alignedExpressionExperiments", alignedEEVos );

        mav.addObject( "numLinkedExpressionExperiments", new Integer( coexpressions.getGeneCoexpressionType()
                .getExpressionExperimentIds().size() ) );
        mav.addObject( "numLinkedPredictedExpressionExperiments", new Integer( coexpressions
                .getPredictedCoexpressionType().getExpressionExperimentIds().size() ) );
        mav.addObject( "numLinkedAlignedExpressionExperiments", new Integer( coexpressions
                .getProbeAlignedCoexpressionType().getExpressionExperimentIds().size() ) );
        //
        // mav.addObject( "numUsedExpressionExperiments", new Long( coexpressions.getGeneCoexpressionType()
        // .getNumberOfUsedExpressonExperiments() ) );
        // mav.addObject( "numUsedPredictedExpressionExperiments", new Long(
        // coexpressions.getPredictedCoexpressionType()
        // .getNumberOfUsedExpressonExperiments() ) );
        // mav.addObject( "numUsedAlignedExpressionExperiments", new Long(
        // coexpressions.getProbeAlignedCoexpressionType()
        // .getNumberOfUsedExpressonExperiments() ) );

        // mav.addObject( "numPositiveCoexpressedGenes", numPositiveCoexpressedGenes );
        // mav.addObject( "numNegativeCoexpressedGenes", numNegativeCoexpressedGenes );
        mav.addObject( "numSearchedExpressionExperiments", numExpressionExperiments );
        // mav.addObject( "numQuerySpecificEEs", coexpressions.getQueryGeneSpecificExpressionExperiments().size() );

        mav.addObject( "minimumCommonQueryGenes", minimumCommonQueryGenes );
        mav.addObject( "numGenes", numGenes );
        mav.addObject( "numPredictedGenes", numPredictedGenes );
        mav.addObject( "numProbeAlignedRegions", numProbeAlignedRegions );

        mav.addObject( "numStringencyGenes", numStringencyGenes );
        mav.addObject( "numStringencyPredictedGenes", numStringencyPredictedGenes );
        mav.addObject( "numStringencyProbeAlignedRegions", numStringencyProbeAlignedRegions );

        // mav.addObject( "numMatchedLinks", numMatchedLinks );
        mav.addObject( "sourceGenesString", commandObject.getGeneListString() );
        mav.addObject( "sourceGenes", commandObject.getSourceGenes() );
        mav.addObject( "sourceGenesDescription", sourceGenesDescription );

        // binding objects
        mav.addObject( "coexpressionSearchCommand", commandObject );
        populateTaxonReferenceData( mav.getModel() );
        mav.addAllObjects( errors.getModel() );

        Long elapsed = watch.getTime();
        watch.stop();
        log.info( "Processing after DAO call (elapsed time): " + elapsed );

        this.saveMessage( request, "Coexpression query took: " + coexpressions.getElapsedWallSeconds() );

        return mav;

    }

    private String buildSourceGenesDescription( Collection<Gene> sourceGenes ) {
        StringBuffer buf = new StringBuffer();
        for ( Iterator iter = sourceGenes.iterator(); iter.hasNext(); ) {
            Gene gene = ( Gene ) iter.next();
            buf.append( gene.getOfficialSymbol() );
            if ( iter.hasNext() ) buf.append( ", " );
        }
        return buf.toString();
    }

    // @SuppressWarnings("unchecked")
    // private Collection<ExpressionExperimentValueObject> retreiveEEFromDB( Collection<Long> eeIds,
    // CoexpressionTypeValueObject coexpressions ) {
    //
    // // This is necessary for security filtering
    // Collection<ExpressionExperimentValueObject> eeVos = expressionExperimentService.loadValueObjects( eeIds );
    //
    // for ( ExpressionExperimentValueObject eeVo : eeVos ) {
    // eeVo.setCoexpressionLinkCount( coexpressions.getLinkCountForEE( eeVo.getId() ) );
    // eeVo.setRawCoexpressionLinkCount( coexpressions.getRawLinkCountForEE( eeVo.getId() ) );
    // eeVo.setSpecific( coexpressions.getExpressionExperiment( eeVo.getId() ).isSpecific() );
    // }
    //
    // List<ExpressionExperimentValueObject> eeList = new ArrayList<ExpressionExperimentValueObject>( eeVos );
    // Collections.sort( eeList, new ExpressionExperimentComparator() );
    // return eeList;
    //
    // }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        // add species
        populateTaxonReferenceData( mapping );

        return mapping;
    }

    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateTaxonReferenceData( Map mapping ) {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxa.add( taxon );
        }
        Collections.sort( taxa, new Comparator<Taxon>() {
            public int compare( Taxon o1, Taxon o2 ) {
                return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
            }
        } );
        mapping.put( "taxa", taxa );
    }

    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /**
     * @param request
     * @param csc
     */
    private void loadCookie( HttpServletRequest request, MultipleCoexpressionSearchCommand csc ) {

        // cookies aren't all that important, if they're missing we just go on.
        if ( request == null || request.getCookies() == null ) return;

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    csc.setEeSearchString( cookie.getString( "eeSearchString" ) );

                    csc.setStringency( cookie.getInt( "stringency" ) );
                    Taxon taxon = taxonService.load( Long.parseLong( cookie.getString( "taxonId" ) ) );
                    csc.setTaxon( taxon );

                    // save the gene name. If the gene id is on, then convert the ID to a gene first
                    String searchString = cookie.getString( "searchString" );
                    csc.setGeneListString( searchString );

                    String id = cookie.getString( "id" );
                    csc.setId( id );

                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
    }

    /**
     * Fills in the command object in the case that GET parameters are passed in
     * 
     * @param request
     * @param csc
     * @see CoexpressionWrapper.extractParameters
     */
    private void loadGETParameters( HttpServletRequest request, MultipleCoexpressionSearchCommand csc ) {
        if ( request == null ) {
            return;
        }
        if ( ( request.getParameter( "searchString" ) == null ) && ( request.getParameter( "id" ) == null ) ) return;

        Map params = request.getParameterMap();

        if ( params.get( "eeSearchString" ) != null ) {
            csc.setEeSearchString( ( ( String[] ) params.get( "eeSearchString" ) )[0] );
        }
        if ( params.get( "stringency" ) != null ) {
            String[] stringency = ( String[] ) params.get( "stringency" );
            Integer num = Integer.parseInt( stringency[0] );
            csc.setStringency( num );
        }
        if ( params.get( "taxon" ) != null ) {
            // can handle scientific name, common name, or id.
            String text = ( ( String[] ) params.get( "taxon" ) )[0];
            Taxon taxon = null;
            try {
                Long id = Long.parseLong( text );
                taxon = taxonService.load( id );
            } catch ( NumberFormatException e ) {
                taxon = taxonService.findByScientificName( text );
            }
            if ( taxon == null ) {
                taxon = taxonService.findByCommonName( text );
            }

            csc.setTaxon( taxon );
        }
        if ( params.get( "searchString" ) != null ) {

            String searchString = ( ( String[] ) params.get( "searchString" ) )[0];

            csc.setGeneListString( searchString );
        }
        if ( params.get( "id" ) != null ) {
            String id = ( ( String[] ) params.get( "id" ) )[0];
            csc.setId( id );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "searchString" ) != null || request.getParameter( "id" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }

        return super.showForm( request, response, errors );
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            final Object command, final MessageUtil messenger, final BindException errors ) {

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, command, messenger, errors ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                // SecurityContextHolder.setContext( securityContext );
                // CoexpressionSearchCommand csc = ( CoexpressionSearchCommand ) command;
                //
                // ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                // .getAuthentication().getName(), "Coexpression analysis for "
                // + csc.getSourceGene().getOfficialSymbol() );
                //
                // job.updateProgress( "Analyzing coexpresson for " + csc.getSourceGene().getOfficialSymbol() );
                // CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                // .getCoexpressedGenes( csc.getSourceGene(), csc.getToUseEE(), csc.getStringency() );
                //
                // StopWatch watch = new StopWatch();
                //
                // watch.start();
                //
                // // get all the coexpressed genes and sort them by dataset count
                // List<CoexpressionValueObject> coexpressedGenes = new ArrayList<CoexpressionValueObject>();
                // coexpressedGenes.addAll( coexpressions.getCoexpressionData() );
                //
                // // sort coexpressed genes by dataset count
                // Collections.sort( coexpressedGenes, new CoexpressionComparator() );
                //
                // computeGoOverlap( csc, coexpressedGenes );
                //
                // // load expression experiment value objects
                // Collection<Long> eeIds = new HashSet<Long>();
                // Collection<ExpressionExperimentValueObject> origEeVos = coexpressions.getExpressionExperiments();
                // for ( ExpressionExperimentValueObject eeVo : origEeVos ) {
                // eeIds.add( eeVo.getId() );
                // }
                //
                // Collection<ExpressionExperimentValueObject> eeVos = expressionExperimentService
                // .loadValueObjects( eeIds );
                // // add link count information to ee value objects
                // // coexpressions.calculateLinkCounts();
                // // coexpressions.calculateRawLinkCounts();
                //
                // for ( ExpressionExperimentValueObject eeVo : eeVos ) {
                // eeVo.setCoexpressionLinkCount( coexpressions.getGeneCoexpressionType().getLinkCountForEE(
                // eeVo.getId() ) );
                // eeVo.setRawCoexpressionLinkCount( coexpressions.getGeneCoexpressionType().getRawLinkCountForEE(
                // eeVo.getId() ) );
                // }
                //
                // // new ModelAndView(getSuccessView());
                //
                // // no genes are coexpressed
                // // return error
                // if ( coexpressedGenes.size() == 0 ) {
                // this.saveMessage( "No genes are coexpressed with the given stringency." );
                // }
                //
                // Long numUsedExpressionExperiments = new Long( coexpressions.getGeneCoexpressionType()
                // .getNumberOfUsedExpressonExperiments() );
                // Long numPositiveCoexpressedGenes = new Long( coexpressions.getGeneCoexpressionType()
                // .getPositiveStringencyLinkCount() );
                // Long numNegativeCoexpressedGenes = new Long( coexpressions.getGeneCoexpressionType()
                // .getNegativeStringencyLinkCount() );
                // Long numGenes = new Long( coexpressions.getNumGenes() );
                // Long numPredictedGenes = new Long( coexpressions.getNumPredictedGenes() );
                // Long numProbeAlignedRegions = new Long( coexpressions.getNumProbeAlignedRegions() );
                // Long numStringencyGenes = new Long( coexpressions.getNumStringencyGenes() );
                // Long numStringencyPredictedGenes = new Long( coexpressions.getNumStringencyPredictedGenes() );
                // Long numStringencyProbeAlignedRegions = new Long( coexpressions.getNumStringencyProbeAlignedRegions()
                // );
                // // Integer numMatchedLinks = coexpressions.getGeneCoexpressionType().getLinkCount();
                //
                // // addTimingInformation( request, coexpressions );
                // job.updateProgress( "ending...." );
                // ProgressManager.destroyProgressJob( job );
                //
                // // request.getParameterMap().remove( "searchString" );
                // // request.setAttribute( "inner", "inner" );
                // ModelAndView mav = new ModelAndView( getSuccessView() );
                // // mav.setViewName( getSuccessView() );
                //
                // mav.addObject( "coexpressedGenes", coexpressedGenes );
                //
                // mav.addObject( "numPositiveCoexpressedGenes", numPositiveCoexpressedGenes );
                // mav.addObject( "numNegativeCoexpressedGenes", numNegativeCoexpressedGenes );
                // mav.addObject( "numSearchedExpressionExperiments", csc.getToUseEE().size() );
                // mav.addObject( "numUsedExpressionExperiments", numUsedExpressionExperiments );
                //
                // mav.addObject( "numGenes", numGenes );
                // mav.addObject( "numPredictedGenes", numPredictedGenes );
                // mav.addObject( "numProbeAlignedRegions", numProbeAlignedRegions );
                //
                // mav.addObject( "numStringencyGenes", numStringencyGenes );
                // mav.addObject( "numStringencyPredictedGenes", numStringencyPredictedGenes );
                // mav.addObject( "numStringencyProbeAlignedRegions", numStringencyProbeAlignedRegions );
                // // mav.addObject( "numSourceGeneGoTerms", numSourceGeneGoTerms );
                //
                // // mav.addObject( "numMatchedLinks", numMatchedLinks );
                // mav.addObject( "sourceGene", csc.getSourceGene() );
                // mav.addObject( "expressionExperiments", eeVos );
                // mav.addObject( "numLinkedExpressionExperiments", new Integer( eeVos.size() ) );
                //
                // // binding objects
                // mav.addObject( "coexpressionSearchCommand", csc );
                // populateTaxonReferenceData( mav.getModel() );
                // mav.addAllObjects( errors.getModel() );
                //
                // Long elapsed = watch.getTime();
                // watch.stop();
                // log.info( "Processing after DAO call (elapsed time): " + elapsed );
                //
                // this.saveMessage( "Coexpression query took: " + coexpressions.getElapsedWallSeconds() );
                //
                // return mav;

                return null;

            }

        };
    }

    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperimentValueObject> retreiveEEFromDB( Collection<Long> eeIds,
            MultipleCoexpressionTypeValueObject coexpressions ) {

        // This is necessary for security filtering
        Collection<ExpressionExperimentValueObject> eeVos = expressionExperimentService.loadValueObjects( eeIds );

        for ( ExpressionExperimentValueObject eeVo : eeVos ) {
            eeVo.setCoexpressionLinkCount( coexpressions.getLinkCountForEE( eeVo.getId() ) );
            eeVo.setRawCoexpressionLinkCount( coexpressions.getRawLinkCountForEE( eeVo.getId() ) );
            eeVo.setSpecific( coexpressions.getExpressionExperiment( eeVo.getId() ).isSpecific() );
        }

        List<ExpressionExperimentValueObject> eeList = new ArrayList<ExpressionExperimentValueObject>( eeVos );
        Collections.sort( eeList, new ExpressionExperimentComparator() );
        return eeList;

    }

    class MultipleCoexpressionSearchCookie extends ConfigurationCookie {

        public MultipleCoexpressionSearchCookie( MultipleCoexpressionSearchCommand command ) {
            super( COOKIE_NAME );

            this.setProperty( "eeSearchString", command.getEeSearchString() );

            this.setProperty( "searchString", command.getGeneListString() );

            this.setProperty( "stringency", command.getStringency() );
            this.setProperty( "taxonId", command.getTaxon().getId() );

            this.setMaxAge( 100000 );
            this.setComment( "Information for coexpression search form" );
        }

    }

    /**
     * @author luke
     */
    class CommonCoexpressionComparator implements Comparator {

        public int compare( Object o1, Object o2 ) {
            CommonCoexpressionValueObject v1 = ( ( CommonCoexpressionValueObject ) o1 );
            CommonCoexpressionValueObject v2 = ( ( CommonCoexpressionValueObject ) o2 );
            int o1Size = v1.getCommonCoexpressedQueryGenes().size();
            int o2Size = v2.getCommonCoexpressedQueryGenes().size();
            if ( o1Size > o2Size ) {
                return -1;
            } else if ( o1Size < o2Size ) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    class ExpressionExperimentComparator implements Comparator {

        public int compare( Object o1, Object o2 ) {
            ExpressionExperimentValueObject v1 = ( ( ExpressionExperimentValueObject ) o1 );
            ExpressionExperimentValueObject v2 = ( ( ExpressionExperimentValueObject ) o2 );
            Long o1Size = v1.getCoexpressionLinkCount();
            Long o2Size = v2.getCoexpressionLinkCount();
            if ( o1Size > o2Size ) {
                return -1;
            } else if ( o1Size < o2Size ) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }
}