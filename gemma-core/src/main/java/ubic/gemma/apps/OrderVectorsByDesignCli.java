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

package ubic.gemma.apps;

import java.util.List;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Use to
 * 
 * @author paul
 * @version $Id$
 */
public class OrderVectorsByDesignCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        OrderVectorsByDesignCli c = new OrderVectorsByDesignCli();

        Exception e = c.doWork( args );

        if ( e != null ) {
            // ...
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "OrderVectorsByDesign", args );

        for ( BioAssaySet ee : this.expressionExperiments ) {

            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }

            ExpressionExperiment ee2 = ( ExpressionExperiment ) ee;
            this.eeService.thawLite( ee2 );

            if ( ee2.getExperimentalDesign().getExperimentalFactors().size() == 0 ) {
                log.info( ee + " does not have a populated experimental design, skipping" );
                continue;
            }

            List<BioMaterial> start = null;

            /*
             * Get the biomaterials. Go by bioassaydimension
             */

            List<BioMaterial> orderByExperimentalDesign = ExpressionDataMatrixColumnSort.orderByExperimentalDesign(
                    start, ee2.getExperimentalDesign().getExperimentalFactors() );

            /*
             * Update the processed vectors.
             */

            /*
             * Recreate the processed vectors
             */

        }

        // TODO Auto-generated method stub
        return null;
    }

}
