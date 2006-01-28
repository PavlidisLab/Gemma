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
package edu.columbia.gemma.web.controller.genome.gene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.gene.GeneService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author daq2101
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geneController" name="/geneDetail.html"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 */
public class GeneController extends BaseCommandController {
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

    @Override
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        Map<String, Object> geneModel = new HashMap<String, Object>();
        long geneID = 0;
        String action = request.getParameter( "action" );
        if ( request.getParameter( "geneID" ) != null )
            geneID = new Long( request.getParameter( "geneID" ) ).longValue();
        if ( geneID == 0 ) throw new Exception( "Error: Must pass geneID parameter" );
        if ( action == null ) action = "view";

        Gene g = this.getGeneService().findByID( geneID );

        if ( action.equals( "addcitation" ) ) {
            String pubmedID = request.getParameter( "pubmedID" );
            BibliographicReference br = this.getBibliographicReferenceService().findByExternalId( pubmedID, "PUBMED" );
            if ( br == null )
                br = this.getBibliographicReferenceService().saveBibliographicReferenceByLookup( pubmedID, "PUBMED" );

            if ( br != null ) {
                Collection<BibliographicReference> cites = g.getCitations();
                cites.add( br );
                g.setCitations( cites );
                this.getGeneService().update( g );
                return new ModelAndView( new RedirectView(
                        "candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId() ) );
            }
        }

        if ( action.equals( "removecitation" ) ) {
            long citationID = new Long( request.getParameter( "citationID" ) ).longValue();
            java.util.Collection cites = g.getCitations();
            BibliographicReference br = null;
            for ( java.util.Iterator iter = cites.iterator(); iter.hasNext(); ) {
                br = ( BibliographicReference ) iter.next();
                if ( br.getId().longValue() == citationID ) {
                    cites.remove( br );
                }
            }
            g.setCitations( cites );
            this.getGeneService().update( g );
            return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?target=geneDetail&geneID="
                    + g.getId() ) );
        }

        if ( action.equals( "updatecitation" ) ) {
            long citationID = new Long( request.getParameter( "citationID" ) ).longValue();
            String description = request.getParameter( "description" );
            Collection<BibliographicReference> cites = g.getCitations();
            for ( BibliographicReference br : cites ) {
                if ( br.getId().longValue() == citationID ) {
                    br.setDescription( description );
                }
            }
            g.setCitations( cites );
            this.getGeneService().update( g );
            return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?target=geneDetail&geneID="
                    + g.getId() ) );

        }
        geneModel.put( "gene", g );
        return new ModelAndView( "geneDetail", "model", geneModel );
    }

}