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
package ubic.gemma.gemmaspaces.expression.experiment;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.gemmaspaces.GemmaSpacesResult;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentReportTaskImpl implements ExpressionExperimentReportTask {

    private ExpressionExperimentReportService expressionExperimentReportService = null;

    private String taskId = null;

    public GemmaSpacesResult execute() {

        expressionExperimentReportService.generateSummaryObjects();

        return null;
    }

    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /**
     * Returns the taskId for this task.
     * 
     * @return
     */
    public String getTaskId() {
        return taskId;
    }

}
