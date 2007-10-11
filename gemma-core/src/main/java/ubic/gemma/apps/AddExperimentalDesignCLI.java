/*
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
package ubic.gemma.apps;

import java.util.HashSet;

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Simple command line to index the gemma db. Can index gene's, Expression experiments or array Designs
 * 
 * @author klc
 * @version $Id$
 */
public class AddExperimentalDesignCLI extends AbstractSpringAwareCLI {

//These were the previous EE ids that had no experimentalDesign
//The easiest way to get this set is to use a sql query like: select ee.id from expression_experiment as ee where ee.experimental_design is null
//    private static final long[] EEwithNoED = { 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215,
//            216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 620, 670, 671, 672,
//            673, 675 };
    
    private static final long[] EEwithNoED = {};

    ExpressionExperimentService eeService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        
        this.eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        AddExperimentalDesignCLI p = new AddExperimentalDesignCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Total run time: " + watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Add Experimental Design", args );
        if ( err != null ) {
            return err;
        }
        try {

            for(Long eeId : EEwithNoED){
                log.info( "Adding experimental design to ee: " + eeId );
                addEd2EE( eeId );
            }
                       

        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    private void addEd2EE( Long eeID ) {

        ExpressionExperiment ee = eeService.load( eeID );
        
        //Is this necessary to test?
        if ( ee.getExperimentalDesign() != null ) return;

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        ed.setName( ee.getName() + " Experimental Design" );
        ed.setTypes( new HashSet<Characteristic>() );

        ee.setExperimentalDesign( ed );
        eeService.update( ee );

    }

}
