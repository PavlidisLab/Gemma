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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.CandidateGene;
import ubic.gemma.model.genome.gene.CandidateGeneList;
import ubic.gemma.model.genome.gene.CandidateGeneListService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author daq2101
 * @version $Id$
 */
@Controller
public class CandidateGeneListController extends BaseCommandController {
    
    @Autowired
    private CandidateGeneListService candidateGeneListService = null;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private UserService userService = null;

    /**
     * @return Returns the candidateGeneListService.
     */
    public CandidateGeneListService getCandidateGeneListService() {
        return candidateGeneListService;
    }

    /**
     * @return Returns the geneService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param UserService The UserService to set.
     */
    public UserService getUserService() {
        return userService;
    }

    @Override
    @SuppressWarnings("unused")
    public ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        Map<String, Object> candidateGeneListModel = new HashMap<String, Object>();
        String view = "candidateGeneList";
        String action = request.getParameter( "action" );
        String target = request.getParameter( "target" );
        long listID = 0;
        long geneID = 0;
        if ( request.getParameter( "listID" ) != null )
            listID = new Long( request.getParameter( "listID" ) ).longValue();
        if ( request.getParameter( "geneID" ) != null )
            geneID = new Long( request.getParameter( "geneID" ) ).longValue();
        if ( action == null ) action = "view";

        // Load the user into usr, used to attribute actions and search by owner
        User usr = userService.findByUserName( request.getRemoteUser() );
        this.candidateGeneListService.setActor( usr );

        if ( target != null ) {
            candidateGeneListModel.put( "listID", request.getParameter( "listID" ) );
            candidateGeneListModel.put( "target", target );
            return new ModelAndView( "candidateGeneListActionComplete", "model", candidateGeneListModel );
        }

        if ( action.compareTo( "view" ) == 0 ) {
            if ( listID > 0 ) {
                // requesting a specific list; next view is Detail
                view = "candidateGeneListDetail";
                candidateGeneListModel
                        .put( "candidateGeneLists", this.getCandidateGeneListService().findByID( listID ) );
            } else {
                String limitToUser = request.getParameter( "limit" );
                if ( limitToUser == null )
                    candidateGeneListModel.put( "candidateGeneLists", this.getCandidateGeneListService().findAll() );
                else
                    candidateGeneListModel.put( "candidateGeneLists", this.getCandidateGeneListService()
                            .findByListOwner( usr ) );
            }
        }
        CandidateGene cg = null;

        if ( action.compareTo( "updatecandidategene" ) == 0 ) {
            CandidateGeneList cgl = this.getCandidateGeneListService().findByID( listID );
            for ( java.util.Iterator iter = cgl.getCandidates().iterator(); iter.hasNext(); ) {
                cg = ( CandidateGene ) iter.next();
                if ( cg.getId().longValue() == geneID ) {
                    break;
                }
            }
            cg.setDescription( request.getParameter( "description" ) );
            cg.getAuditTrail().update( "CandidateGene Modified Description", usr );
            this.getCandidateGeneListService().updateCandidateGeneList( cgl );
            return new ModelAndView( new RedirectView(
                    "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
                            + request.getParameter( "listID" ) ) );
        }

        if ( action.compareTo( "movecandidateuponcandidatelist" ) == 0 ) {
            CandidateGeneList cgl = this.getCandidateGeneListService().findByID( listID );
            for ( java.util.Iterator iter = cgl.getCandidates().iterator(); iter.hasNext(); ) {
                cg = ( CandidateGene ) iter.next();
                if ( cg.getId().longValue() == geneID ) break;
            }
            cgl.increaseRanking( cg );
            cg.getAuditTrail().update( "CandidateGene Increased Rank", usr );
            this.getCandidateGeneListService().updateCandidateGeneList( cgl );
            return new ModelAndView( new RedirectView(
                    "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
                            + request.getParameter( "listID" ) ) );
        }

        if ( action.compareTo( "movecandidatedownoncandidatelist" ) == 0 ) {
            CandidateGeneList cgl = this.getCandidateGeneListService().findByID( listID );
            for ( java.util.Iterator iter = cgl.getCandidates().iterator(); iter.hasNext(); ) {
                cg = ( CandidateGene ) iter.next();
                if ( cg.getId().longValue() == geneID ) break;
            }
            cgl.decreaseRanking( cg );
            cg.getAuditTrail().update( "CandidateGene Decreased Rank", usr );
            this.getCandidateGeneListService().updateCandidateGeneList( cgl );
            return new ModelAndView( new RedirectView(
                    "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
                            + request.getParameter( "listID" ) ) );
        }

        if ( action.compareTo( "addgenetocandidatelist" ) == 0 ) {
            CandidateGeneList cgl = this.getCandidateGeneListService().findByID( listID );
            Gene g = this.getGeneService().load( geneID );
            cg = cgl.addCandidate( g );
            cg.setAuditTrail( AuditTrail.Factory.newInstance() );
            cg.getAuditTrail().start( "CandidateGene Created.", usr );
            cg.setOwner( usr );
            cg.setName( cg.getGene().getName() );
            cg.setDescription( "" );
            this.getCandidateGeneListService().updateCandidateGeneList( cgl );
            return new ModelAndView( new RedirectView(
                    "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
                            + request.getParameter( "listID" ) ) );
        }

        if ( action.compareTo( "removegenefromcandidatelist" ) == 0 ) {
            CandidateGeneList cgl = this.getCandidateGeneListService().findByID( listID );
            for ( java.util.Iterator iter = cgl.getCandidates().iterator(); iter.hasNext(); ) {
                cg = ( CandidateGene ) iter.next();
                if ( cg.getId().longValue() == geneID ) break;
            }
            cgl.removeCandidate( cg );
            this.getCandidateGeneListService().updateCandidateGeneList( cgl );
            return new ModelAndView( new RedirectView(
                    "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
                            + request.getParameter( "listID" ) ) );
        }

        if ( action.compareTo( "update" ) == 0 ) {
            CandidateGeneList cgl = this.getCandidateGeneListService().findByID( listID );
            cgl.setName( request.getParameter( "listName" ).toString() );
            cgl.setDescription( request.getParameter( "listDescription" ).toString() );
            this.getCandidateGeneListService().updateCandidateGeneList( cgl );
            return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?listID="
                    + request.getParameter( "listID" ) ) );
        }

        if ( action.compareTo( "delete" ) == 0 ) {
            this.getCandidateGeneListService().removeCandidateGeneList(
                    this.getCandidateGeneListService().findByID( listID ) );
            return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm" ) );

        }

        if ( action.compareTo( "add" ) == 0 ) {
            String newName = request.getParameter( "newName" );
            CandidateGeneList cgl = this.getCandidateGeneListService().createByName( newName );
            return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm" ) );

        }
        return new ModelAndView( view, "model", candidateGeneListModel );
    }

    /**
     * @param candidateGeneListService The candidateGeneListService to set.
     */
    public void setCandidateGeneListService( CandidateGeneListService candidateGeneListService ) {
        this.candidateGeneListService = candidateGeneListService;
    }

    /**
     * @param candidateGeneListService The candidateGeneListService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param userService The UserService to set.
     */
    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }

}
