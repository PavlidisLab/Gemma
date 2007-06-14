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

import ubic.gemma.gemmaspaces.AbstractGemmaSpacesService;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;

/**
 * This service is used to generate the expression experiment reports on a gemma spaces compute server.
 * 
 * @author keshav
 * @version $Id$
 */
public class GemmaSpacesExpressionExperimentReportService extends AbstractGemmaSpacesService {

    // TODO Add these xdoclet tags to class javadoc
    // @spring.bean id="gemmaSpacesExpressionReportService"
    // @spring.property name="gigaSpacesUtil" ref="gigaSpacesUtil"

    public void generateSummaryObjects() {

        ExpressionExperimentReportTask reportProxy = ( ExpressionExperimentReportTask ) updatedContext
                .getBean( "expressionExperimentReportTask" );
        reportProxy.execute();
    }

    @Override
    protected void setGigaSpacesUtil( GemmaSpacesUtil gigaSpacesUtil ) {
        this.injectGigaspacesUtil( gigaSpacesUtil );

    }

}
