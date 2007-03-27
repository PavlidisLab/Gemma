/*
 * The Gemma project
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
package ubic.gemma.web.controller.expression.designElement;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.genome.CompositeSequenceGeneMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * @author keshav
 * @author paul
 * @version $Id$
 * @spring.bean id="designElementController"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name="methodNameResolver" ref="designElementActions"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="arrayDesignMapResultService" ref="arrayDesignMapResultService"
 * @spring.property name="compositeSequenceGeneMapperService" ref="compositeSequenceGeneMapperService"
 * @spring.property name="searchService" ref="searchService"
 */
public class DesignElementController extends BaseMultiActionController {

    private SearchService searchService;
    private ArrayDesignService arrayDesignService = null;
    private ArrayDesignMapResultService arrayDesignMapResultService;
    private CompositeSequenceService compositeSequenceService;
    private CompositeSequenceGeneMapperService compositeSequenceGeneMapperService;

    /**
     * @param compositeSequenceGeneMapperService the compositeSequenceGeneMapperService to set
     */
    public void setCompositeSequenceGeneMapperService(
            CompositeSequenceGeneMapperService compositeSequenceGeneMapperService ) {
        this.compositeSequenceGeneMapperService = compositeSequenceGeneMapperService;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );
        String arid = request.getParameter( "arid" );

        ArrayDesign arrayDesign = null;
        if ( arid != null ) {
            try {
                arrayDesign = arrayDesignService.load( Long.parseLong( arid ) );
            } catch ( NumberFormatException e ) {
                // Fail gracefull, please.
            }
        }

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            this.saveMessage( request, "No search critera provided" );
            // return showAll( request, response );
        }

        /*
         * There have to be a few ways of searching: - by ID, by bioSequence, by Gene name. An array design may or may
         * not be given.
         */

        Collection<CompositeSequence> searchResults = searchService.compositeSequenceSearch( filter, arrayDesign );
        Collection<Gene> geneResults = null;
        try {
            geneResults = searchService.geneSearch( filter );
        } catch ( Exception e ) {
            // fail quietly
        }
        // if there have been any genes returned, find the compositeSequences associated with the genes
        if ( geneResults != null && geneResults.size() > 0 ) {
            for ( Gene gene : geneResults ) {

                if ( arrayDesign == null ) {
                    Collection<CompositeSequence> geneCs = compositeSequenceGeneMapperService
                            .getCompositeSequencesByGeneId( gene.getId() );
                    searchResults.addAll( geneCs );
                } else {
                    Collection<CompositeSequence> geneCs = compositeSequenceGeneMapperService.getCompositeSequences(
                            gene, arrayDesign );
                    searchResults.addAll( geneCs );
                }

            }
        }

        Collection<CompositeSequenceMapValueObject> compositeSequenceSummary;
        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            this.saveMessage( request, "Your search yielded no results" );
            compositeSequenceSummary = new ArrayList<CompositeSequenceMapValueObject>();
            // return showAll( request, response );
        } else {
            this.saveMessage( request, searchResults.size() + " probes matched your search." );
            Collection rawSummaries = compositeSequenceService.getRawSummary( searchResults, 100 );
            compositeSequenceSummary = arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
        }

        ModelAndView mav = new ModelAndView( "arrayDesign.compositeSequences" );
        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "sequenceData", compositeSequenceSummary );
        mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        this.saveMessage( request, "Search Criteria: " + filter );

        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
    // String name = request.getParameter( "name" );
    //
    // if ( name == null ) {
    // // should be a validation error, on 'submit'.
    // throw new EntityNotFoundException( "Must provide an Array Design name" );
    // }
    //
    // DesignElement designElement = designElementService.findArrayDesignByName( name );
    // if ( designElement == null ) {
    // throw new EntityNotFoundException( name + " not found" );
    // }
    //
    // this.addMessage( request, "designElement.found", new Object[] { name } );
    // request.setAttribute( "name", name );
    // return new ModelAndView( "designElement.detail" ).addObject( "designElement", designElement );
    // }
    /**
     * Disabled for now.
     * 
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings( { "unused" })
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        this.addMessage( request, "object.unavailable", null );
        return new ModelAndView( "mainMenu" );
        /*
         * log.debug( "entered showAll from " + request.getRequestURI() ); String name = request.getParameter( "name" );
         * ArrayDesign ad = arrayDesignService.findArrayDesignByName( name ); Collection<CompositeSequence> ads =
         * ad.getCompositeSequences(); // FIXME this only works on composite sequences. return new ModelAndView(
         * "designElements" ).addObject( "designElements", ads );
         */
    }

    /**
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
    // String name = request.getParameter( "name" );
    //
    // if ( name == null ) {
    // // should be a validation error.
    // throw new EntityNotFoundException( "Must provide a name" );
    // }
    //
    // DesignElement designElement = designElementService.findDesignElementByName( name );
    // if ( designElement == null ) {
    // throw new EntityNotFoundException( designElement + " not found" );
    // }
    //
    // return doDelete( request, designElement );
    // }
    /**
     * @param request
     * @param locale
     * @param bibRef
     * @return
     */
    // private ModelAndView doDelete( HttpServletRequest request, DesignElement designElement ) {
    // designElementService.remove( designElement );
    // log.info( "Bibliographic reference with pubMedId: " + designElement.getName() + " deleted" );
    // addMessage( request, "designElement.deleted", new Object[] { designElement.getName() } );
    // return new ModelAndView( "designElements", "designElement", designElement );
    // }
    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    public void setArrayDesignMapResultService( ArrayDesignMapResultService arrayDesignMapResultService ) {
        this.arrayDesignMapResultService = arrayDesignMapResultService;
    }

}
