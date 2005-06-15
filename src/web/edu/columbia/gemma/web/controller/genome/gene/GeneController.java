package edu.columbia.gemma.web.controller.genome.gene;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;


import edu.columbia.gemma.genome.gene.CandidateGeneList;
import edu.columbia.gemma.genome.gene.CandidateGeneListService;
import edu.columbia.gemma.genome.gene.GeneService;
import edu.columbia.gemma.genome.Gene;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="geneController" name="/geneDetail.htm"
 * @spring.property name="geneService" ref="geneService"
 */
public class GeneController extends BaseCommandController {
	private GeneService geneService = null;

	/**
	 * @return Returns the geneService.
	 */
	public GeneService getGeneService() {
		return geneService;
	}

	/**
	 * @param geneService
	 *            The geneService to set.
	 */
	public void setGeneService(GeneService geneService) {
		this.geneService = geneService;
	}
	public ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Map geneModel = new HashMap();
		long geneID=0;
		if (request.getParameter("geneID") != null)
			geneID = new Long(request.getParameter("geneID")).longValue();
		if( geneID==0)
			throw new Exception("Error: Must pass geneID parameter");
		
		Gene g = this.getGeneService().findByID(geneID);
		geneModel.put("gene", g);
		return new ModelAndView("geneDetail", "model", geneModel);
	}

}