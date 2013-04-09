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
package ubic.gemma.web.controller.common.rss;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

@Controller
public class RssFeedController {
	
	@Autowired private WhatsNewService whatsNewService;
    
	public RssFeedController() {
	    // Dummy default constructor
	}
	
	public RssFeedController(WhatsNewService whatsNewService) {
	    this.whatsNewService = whatsNewService;
	}
	
	/**
     * Show all experiments
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */
	@RequestMapping(value = {"/rssfeed"}, method = RequestMethod.GET)
	public ModelAndView getLatestExperiments( HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		if (ConfigUtils.getBoolean("gemma.rss.enabled"))
		{
			WhatsNew wn = whatsNewService.retrieveReport();
			if (wn == null) {
				Calendar c = Calendar.getInstance();
				Date date = c.getTime();
				date = DateUtils.addWeeks(date, -1);
				wn = whatsNewService.getReport(date);
			}
			mav.setViewName("rssViewer");
			
			int updatedExperimentsCount = 0;
			int newExperimentsCount = 0;
			Map<ExpressionExperiment, String> experiments = new HashMap<ExpressionExperiment, String>();
			
			if (wn != null)
			{
				Collection<ExpressionExperiment> updatedExperiments = wn.getUpdatedExpressionExperiments();
				Collection<ExpressionExperiment> newExperiments = wn.getNewExpressionExperiments();
				
				for (ExpressionExperiment e : updatedExperiments)
				{
					experiments.put(e, "Updated");
				}
				for (ExpressionExperiment e : newExperiments)
				{
					experiments.put(e, "New");
				}
				
				updatedExperimentsCount = updatedExperiments.size();
				newExperimentsCount = newExperiments.size();
			}
			
			mav.addObject("feedContent", experiments);
			mav.addObject("updateCount", updatedExperimentsCount);
			mav.addObject("newCount", newExperimentsCount);
		}
		return mav;
	}
	

}
