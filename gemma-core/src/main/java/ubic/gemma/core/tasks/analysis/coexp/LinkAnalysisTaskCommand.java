/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.core.tasks.analysis.coexp;

import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Command object for Link analysis
 *
 * @author Paul
 */
public class LinkAnalysisTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;
    private final FilterConfig filterConfig;
    private final LinkAnalysisConfig linkAnalysisConfig;
    private ExpressionExperiment expressionExperiment;

    public LinkAnalysisTaskCommand( ExpressionExperiment ee, LinkAnalysisConfig lac, FilterConfig fg ) {
        super();
        this.expressionExperiment = ee;
        this.filterConfig = fg;
        this.linkAnalysisConfig = lac;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    @SuppressWarnings("unused") // Possible external use
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public FilterConfig getFilterConfig() {
        return this.filterConfig;
    }

    public LinkAnalysisConfig getLinkAnalysisConfig() {
        return this.linkAnalysisConfig;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return LinkAnalysisTask.class;
    }

}
