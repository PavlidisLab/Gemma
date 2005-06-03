package edu.columbia.gemma.web.controller.genome.gene;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import edu.columbia.gemma.genome.gene.CandidateGeneListService;
import edu.columbia.gemma.genome.gene.CandidateGeneList;
import edu.columbia.gemma.genome.gene.CandidateGene;
/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="candidateGeneListController" name="/candidateGeneList.htm /candidateGeneListDetail.htm /candidateGeneListActionComplete.htm"
 * @spring.property name="candidateGeneListService" ref="candidateGeneListService"
 */
public class CandidateGeneListController extends BaseCommandController {
    private CandidateGeneListService candidateGeneListService;

    /**
     * @return Returns the candidateGeneListService.
     */
    public CandidateGeneListService getCandidateGeneListService() {
        return candidateGeneListService;
    }

    /**
     * @param candidateGeneListService The candidateGeneListService to set.
     */
    public void setCandidateGeneListService( CandidateGeneListService candidateGeneListService) {
        this.candidateGeneListService = candidateGeneListService;
    }
    
    public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
    throws Exception{
    	
        Map candidateGeneListModel = new HashMap();
        String view = "candidateGeneList";
        String action = request.getParameter("action");
        String lid = request.getParameter("listID");
        String target = request.getParameter("target");
        if( action==null )
        	action = "view";

        if( target != null){
        	candidateGeneListModel.put("listID", lid);
        	candidateGeneListModel.put("target", target);
        	return new ModelAndView( "candidateGeneListActionComplete", "model", candidateGeneListModel );	
        }
        
        if( action.compareTo("view")==0 ){
        	if( lid != null && lid != ""){
		    	// requesting a specific list; next view is Detail
		    	view = "candidateGeneListDetail";
		    	long listID = new Long(lid).longValue();
		    	candidateGeneListModel.put("candidateGeneLists", this.getCandidateGeneListService().findByID(listID));
		    }
		    else{
		    	candidateGeneListModel.put("candidateGeneLists", this.getCandidateGeneListService().getAll());
		    }
        }
        
        if( action.compareTo("addgenetocandidatelist")==0){
        	long listID = new Long(request.getParameter("listID")).longValue();
        	long geneID = new Long(request.getParameter("geneID")).longValue();
        	CandidateGeneList cgl = this.getCandidateGeneListService().findByID(listID);
        	CandidateGene cg = this.getCandidateGeneListService().addCandidateToCandidateGeneList(cgl,geneID);
        	cg.setName(cg.getGene().getName());
        	cg.setDescription("");
        	this.getCandidateGeneListService().saveCandidateGeneList(cgl);
        	return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID=" + lid ) );  
        }

        if( action.compareTo("removegenefromcandidatelist")==0){
        	long listID = new Long(request.getParameter("listID")).longValue();
        	long cgID = new Long(request.getParameter("geneID")).longValue();
        	CandidateGeneList cgl = this.getCandidateGeneListService().findByID(listID);
        	Collection cans = cgl.getCandidates();
        	java.util.Iterator iter = cans.iterator();
        	CandidateGene cg = null;
        	// find the correct CandidateGene in the CandidateGeneList
        	while(iter.hasNext()){
        		cg = (CandidateGene)iter.next();
        		if(cg.getId().longValue() == cgID)
        			break;
        	}
        	cgl.removeCandidate(cg);
        	this.getCandidateGeneListService().saveCandidateGeneList(cgl);
        	return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID=" + lid ) );  
        }
        
        if( action.compareTo("update")==0){
        	long listID = new Long(request.getParameter("listID")).longValue();
        	CandidateGeneList cgl = this.getCandidateGeneListService().findByID(listID);
        	cgl.setName(request.getParameter("listName").toString());
        	cgl.setDescription(request.getParameter("listDescription").toString());
        	this.getCandidateGeneListService().saveCandidateGeneList(cgl);
        	return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm?listID=" + lid ) );   
        }

        if( action.compareTo("delete")==0 ) {
        	long listID = new Long(lid).longValue();
        	this.getCandidateGeneListService().removeCandidateGeneList( this.getCandidateGeneListService().findByID(listID) );
        	view = "candidateGeneListActionComplete";
        	// view remaining lists
        	candidateGeneListModel.put("candidateGeneLists", this.getCandidateGeneListService().getAll());
        	return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm") );   
            
        }
        
        if( action.compareTo("add")==0 ){
        	String newName = request.getParameter("newName");
        	this.getCandidateGeneListService().createCandidateGeneList(newName);
        	view = "candidateGeneListActionComplete";
        	candidateGeneListModel.put("candidateGeneLists", this.getCandidateGeneListService().getAll());
        	return new ModelAndView( new RedirectView( "candidateGeneListActionComplete.htm" ) );   
            
        }
        return new ModelAndView( view, "model", candidateGeneListModel );	
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
