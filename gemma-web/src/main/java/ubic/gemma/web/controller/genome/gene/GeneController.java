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
package ubic.gemma.web.controller.genome.gene;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 * @version $Id$
 * @spring.bean id="geneController"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="methodNameResolver" ref="geneActions"
 */
public class GeneController extends BaseMultiActionController {
    private GeneService geneService = null;
    private BibliographicReferenceService bibliographicReferenceService = null;

    /**
     * @return Returns the geneService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /*
     * @Override @SuppressWarnings( { "unused", "unchecked" }) public ModelAndView handleRequestInternal(
     * HttpServletRequest request, HttpServletResponse response ) throws Exception { Map<String, Object> geneModel =
     * new HashMap<String, Object>(); long geneID = 0; String action = request.getParameter( "action" ); if (
     * request.getParameter( "geneID" ) != null ) geneID = new Long( request.getParameter( "geneID" ) ).longValue(); if (
     * geneID == 0 ) throw new Exception( "Error: Must pass geneID parameter" ); if ( action == null ) action = "view";
     * Gene g = this.getGeneService().findByID( geneID ); if ( action.equals( "addcitation" ) ) { String pubmedID =
     * request.getParameter( "pubmedID" ); BibliographicReference br =
     * this.getBibliographicReferenceService().findByExternalId( pubmedID, "PUBMED" ); // if ( br == null ) br =
     * this.getBibliographicReferenceService().create( pubmedID, "PUBMED" ); if ( br != null ) { Collection<BibliographicReference>
     * cites = g.getCitations(); cites.add( br ); g.setCitations( cites ); this.getGeneService().update( g ); return new
     * ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId() ) ); } }
     * if ( action.equals( "removecitation" ) ) { long citationID = new Long( request.getParameter( "citationID" )
     * ).longValue(); java.util.Collection cites = g.getCitations(); BibliographicReference br = null; for (
     * java.util.Iterator iter = cites.iterator(); iter.hasNext(); ) { br = ( BibliographicReference ) iter.next(); if (
     * br.getId().longValue() == citationID ) { cites.remove( br ); } } g.setCitations( cites );
     * this.getGeneService().update( g ); return new ModelAndView( new RedirectView(
     * "candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId() ) ); } if ( action.equals(
     * "updatecitation" ) ) { long citationID = new Long( request.getParameter( "citationID" ) ).longValue(); String
     * description = request.getParameter( "description" ); Collection<BibliographicReference> cites =
     * g.getCitations(); for ( BibliographicReference br : cites ) { if ( br.getId().longValue() == citationID ) {
     * br.setDescription( description ); } } g.setCitations( cites ); this.getGeneService().update( g ); return new
     * ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId() ) ); }
     * geneModel.put( "gene", g ); return new ModelAndView( "geneDetail", "model", geneModel ); }
     */
    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<Gene> genes = new ArrayList<Gene>();
        // if no IDs are specified, then show an error message
        if ( sId == null ) {
            addMessage( request, "object.notfound", new Object[] { "All genes cannot be listed. Genes " } );
        }

        // if ids are specified, then display only those genes
        else {
            String[] idList = StringUtils.split( sId, ',' );

            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                Gene gene = geneService.load( id );
                if ( gene == null ) {
                    addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                }
                genes.add( gene );
            }
        }
        return new ModelAndView( "genes" ).addObject( "genes", genes );

    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "mainMenu.html" );
        }
        ModelAndView mav = new ModelAndView("gene.detail");
        mav.addObject( "gene", gene );
        Long compositeSequenceCount = geneService.getCompositeSequenceCountById( id );
        mav.addObject( "compositeSequenceCount", compositeSequenceCount );
        return mav;
    }
    
    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "mainMenu.html" );
        }
        ModelAndView mav = new ModelAndView("compositeSequences");
        mav.addObject( "gene", gene );
        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( id );
        mav.addObject( "compositeSequences", compositeSequences );
        return mav;
    }
    
    
}