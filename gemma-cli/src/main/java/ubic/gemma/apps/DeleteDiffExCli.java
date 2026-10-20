/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

/**
 * Delete differential expression analsyes on a per-experiment basis.
 *
 * @author paul
 */
public class DeleteDiffExCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private DifferentialExpressionAnalysisService deas;

    @Override
    public String getCommandName() {
        return "deleteDiffEx";
    }

    @Override
    public String getShortDesc() {
        return "Delete differential expression analyses for experiment(s) from the system";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        log.info( "--------- Deleting any diff ex analyses for " + ee + " --------" );
        deas.removeForExperiment( ee, true );
        refreshExpressionExperimentFromGemmaWebSilently( ee, false, true );
        addSuccessObject( ee, "Deleted differential expression analyses." );
        log.info( "--------- Finished Deleting for " + ee + " -------" );
    }
}
