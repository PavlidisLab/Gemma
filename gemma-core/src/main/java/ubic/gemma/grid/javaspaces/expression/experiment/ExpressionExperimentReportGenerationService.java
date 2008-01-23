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
package ubic.gemma.grid.javaspaces.expression.experiment;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.grid.javaspaces.AbstractSpacesService;
import ubic.gemma.grid.javaspaces.SpacesCommand;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;

/**
 * @author keshav
 * @version $Id: ExpressionExperimentReportGenerationService.java,v 1.2
 *          2007/11/28 18:22:02 keshav Exp $ *
 * @spring.bean name="expressionExperimentReportGenerationService"
 * @spring.property name="expressionExperimentReportService"
 *                  ref="expressionExperimentReportService"
 * 
 */
public class ExpressionExperimentReportGenerationService extends
		AbstractSpacesService {

	private ExpressionExperimentReportService expressionExperimentReportService = null;

	/**
	 * 
	 * 
	 */
	public void generateSummaryObjects() {
		startJob(SpacesEnum.DEFAULT_SPACE.getSpaceUrl(),
				ExpressionExperimentReportTask.class.getName(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ubic.gemma.grid.javaspaces.AbstractSpacesService#runLocally(java.lang.String)
	 */
	@Override
	public void runLocally(String taskId, Object command) {
		expressionExperimentReportService.generateSummaryObjects();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ubic.gemma.grid.javaspaces.AbstractSpacesService#runRemotely(java.lang.String)
	 */
	@Override
	public void runRemotely(String taskId, Object command) {
		// ExpressionExperimentReportTask reportProxy = (
		// ExpressionExperimentReportTask ) updatedContext
		// .getBean( "expressionExperimentReportTask" );
		// reportProxy.execute();

		ExpressionExperimentReportTask reportProxy = (ExpressionExperimentReportTask) updatedContext
				.getBean("proxy");
		SpacesCommand spacesCommand = new SpacesCommand(taskId);
		reportProxy.execute(spacesCommand);
	}

	/**
	 * @param expressionExperimentReportService
	 */
	public void setExpressionExperimentReportService(
			ExpressionExperimentReportService expressionExperimentReportService) {
		this.expressionExperimentReportService = expressionExperimentReportService;
	}

}
