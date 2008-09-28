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
package ubic.gemma.grid.javaspaces.diff;

import java.util.Collection;

import net.jini.space.JavaSpace;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.SpacesTask;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * A task interface to wrap differential expression type jobs. Tasks of this type are submitted to a {@link JavaSpace}
 * and taken from the space by a worker, run on a compute server, and the results are returned to the space.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisTask extends BaseSpacesTask implements
        SpacesTask<SpacesDifferentialExpressionAnalysisCommand> {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;
    private SpacesDifferentialExpressionAnalysisCommand command;

    /**
     * @param command
     * @param differentialExpressionAnalyzerService
     */
    public DifferentialExpressionAnalysisTask( SpacesDifferentialExpressionAnalysisCommand command,
            DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService ) {
        this.command = command;
        this.differentialExpressionAnalyzerService = differentialExpressionAnalyzerService;
    }

    /**
     * @param jsDiffAnalysisCommand
     * @return
     */
    public SpacesResult execute( SpacesDifferentialExpressionAnalysisCommand jsDiffAnalysisCommand ) {
        super.initProgressAppender( this.getClass() );

        this.taskId = TaskRunningService.generateTaskId();

        Collection<DifferentialExpressionAnalysis> result = differentialExpressionAnalyzerService
                .executeTask( this.command );

        SpacesResult r = new SpacesResult( this.taskId );
        r.setAnswer( result );

        return r;
    }
}
