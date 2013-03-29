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
package ubic.gemma.web.spring.integration.twitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.ConfigUtils;

import com.ibm.icu.util.Calendar;

/**
 * @author sshao
 *
 */
@Component
public class TwitterOutbound {
	private static Log log = LogFactory.getLog( TwitterOutbound.class.getName() );
	
	@Autowired private WhatsNewService whatsNewService;
	@Autowired private ExpressionExperimentService expressionExperimentService;
	@Autowired private SecurityService securityService;
	
	@Secured( { "GROUP_AGENT" })
	public void sendDailyFeed() {
		String feed = generateDailyFeed();
		if (ConfigUtils.getBoolean("gemma.twitter.enabled") && !feed.isEmpty())
		{
			log.info(feed);
			ApplicationContext context = new ClassPathXmlApplicationContext("/twitter-outbound.xml", TwitterOutbound.class);
			MessageChannel input = context.getBean("twitterOutbound", MessageChannel.class);
			Message<String> message = new GenericMessage<String>(feed);
	        input.send( message );
		}
	}
	
	private String generateDailyFeed() {
		
		Calendar c = Calendar.getInstance();
		Date date = c.getTime();
		date = DateUtils.addDays(date, -1);
		WhatsNew whatsNew = whatsNewService.getReport(date);
		
		Collection<ExpressionExperiment> experiments = new ArrayList<ExpressionExperiment>();
		int updatedExperimentsCount = 0;
		int newExperimentsCount = 0;
		
		Random rand = new Random();
	
		// Query for all updated / new expression experiments to store into a experiments collection
		if (whatsNew != null)
		{
			Collection<ExpressionExperiment> updatedExperiments = whatsNew.getUpdatedExpressionExperiments();
			Collection<ExpressionExperiment> newExperiments = whatsNew.getNewExpressionExperiments();
			experiments.addAll(updatedExperiments);
			experiments.addAll(newExperiments);
			updatedExperimentsCount = updatedExperiments.size();
			newExperimentsCount = newExperiments.size();
		}
		
		String status = "";
		ExpressionExperiment experiment;
		
		// Query latest experiments if there are no updated / new experiments
		if (updatedExperimentsCount == 0 && newExperimentsCount == 0)
		{
			Collection<ExpressionExperiment> latestExperiments = expressionExperimentService.findByUpdatedLimitWithAgent(10);
			Collection<Securable> publicExperiments = securityService.choosePublic(latestExperiments);
			
			experiment = (ExpressionExperiment) publicExperiments.toArray()[rand.nextInt(publicExperiments.size())];
		}
		else
		{
			experiment = (ExpressionExperiment) experiments.toArray()[rand.nextInt(experiments.size())];
		}
		
		status = statusWithExperiment(experiment.getName(), updatedExperimentsCount, newExperimentsCount);
		
		// Regenerate status if larger than Twitter required length of 140 characters
		if (status.length() > 140)
		{
			status = statusWithExperiment(experiment.getName().substring(0, status.length()-140), updatedExperimentsCount, newExperimentsCount);
		}
		return status;
	}
	
	/**
	 * 
	 * @param experimentName
	 * @param updatedExperimentsCount
	 * @param newExperimentsCount
	 * @return a status that provides the number of updated and new experiments, a randomly chosen experiment and a link back to Gemma
	 */
	private String statusWithExperiment(String experimentName, int updatedExperimentsCount, int newExperimentsCount)
	{
		if (updatedExperimentsCount == 0 && newExperimentsCount == 0)
		{
			return "Experiment of the day: "+experimentName+"... See all latest experiments at www.chibi.ubc.ca/Gemma/rssfeed";
		}
		else
		{
			return "Experiment of the day: "+experimentName+"... See all "+updatedExperimentsCount+" updated and "+newExperimentsCount+" new experiments at www.chibi.ubc.ca/Gemma/rssfeed";
		}
	}
}
