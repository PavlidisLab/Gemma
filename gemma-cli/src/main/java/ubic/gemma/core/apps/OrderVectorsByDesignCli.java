/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.core.apps;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

/**
 * Use to change the order of the values to match the experimental design.
 *
 * @author paul
 */
public class OrderVectorsByDesignCli extends ExpressionExperimentManipulatingCLI {

    @Override
    public String getCommandName() {
        return "orderVectorsByDesign";
    }

    @Override
    protected void doWork() throws Exception {
        ProcessedExpressionDataVectorService processedExpressionDataVectorService = this
                .getBean( ProcessedExpressionDataVectorService.class );

        for ( BioAssaySet ee : this.expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                processedExpressionDataVectorService.reorderByDesign( ( ExpressionExperiment ) ee );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Experimental: reorder the vectors by experimental design, to save computation later.";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

}
