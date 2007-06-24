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
import ubic.gemma.gemmaspaces.AbstractGemmaSpacesService;
import ubic.gemma.util.gemmaspaces.GemmaSpacesEnum;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;

/**
 * @author keshav
 * @version $Id$ *
 * @spring.bean name="expressionExperimentReportGenerationService"
 * @spring.property name="expressionExperimentReportService" ref="expressionExperimentReportService"
 * @spring.property name="gemmaSpacesUtil" ref="gemmaSpacesUtil"
 */
public class ExpressionExperimentReportGenerationService extends AbstractGemmaSpacesService {

    private ExpressionExperimentReportService expressionExperimentReportService = null;

    /**
     * 
     *
     */
    public void generateSummaryObjects() {
        startJob( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentReportTask.class.getName(), true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.gemmaspaces.AbstractGemmaSpacesService#runLocally(java.lang.String)
     */
    public void runLocally( String taskId ) {
        expressionExperimentReportService.generateSummaryObjects();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.gemmaspaces.AbstractGemmaSpacesService#runRemotely(java.lang.String)
     */
    public void runRemotely( String taskId ) {
        // ExpressionExperimentReportTask reportProxy = ( ExpressionExperimentReportTask ) updatedContext
        // .getBean( "expressionExperimentReportTask" );
        // reportProxy.execute();

        ExpressionExperimentReportService reportProxy = ( ExpressionExperimentReportService ) updatedContext
                .getBean( "expressionExperimentReportService" );
        reportProxy.execute();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.gemmaspaces.AbstractGemmaSpacesService#setGemmaSpacesUtil(ubic.gemma.util.gemmaspaces.GemmaSpacesUtil)
     */
    @Override
    public void setGemmaSpacesUtil( GemmaSpacesUtil gemmaSpacesUtil ) {
        super.injectGemmaSpacesUtil( gemmaSpacesUtil );
    }

    /**
     * @param expressionExperimentReportService
     */
    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

}
