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

import ubic.gemma.core.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Use to change the order of the values to match the experimental design.
 * 
 * @author paul
 *
 */
public class OrderVectorsByDesignCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        OrderVectorsByDesignCli c = new OrderVectorsByDesignCli();

        Exception e = c.doWork( args );

        if ( e != null ) {
            throw ( new RuntimeException( e ) );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "orderVectorsByDesign";
    }

    @Override
    public String getShortDesc() {
        return "Experimental: reorder the vectors by experimental design, to save computation later.";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( args );
        ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService = getBean( ProcessedExpressionDataVectorCreateService.class );

        for ( BioAssaySet ee : this.expressionExperiments ) {

            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }
            ee = this.eeService.thawLite( ( ExpressionExperiment ) ee );
            processedExpressionDataVectorCreateService.reorderByDesign( ee.getId() );

        }

        return null;
    }

}
