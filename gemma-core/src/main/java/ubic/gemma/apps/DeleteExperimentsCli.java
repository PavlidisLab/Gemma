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

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Delete one or more experiments from the system.
 * 
 * @author paul
 * @version $Id$
 */
public class DeleteExperimentsCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        DeleteExperimentsCli d = new DeleteExperimentsCli();
        d.doWork( args );
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "Delete experiments", args );

        for ( BioAssaySet bas : this.expressionExperiments ) {
            this.eeService.delete( ( ExpressionExperiment ) bas );
        }

        return null;
    }

}
