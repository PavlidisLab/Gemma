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
package ubic.gemma.javaspaces.gigaspaces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentMasterCLI extends AbstractSpringAwareCLI {

    private static Log log = LogFactory.getLog( ExpressionExperimentMasterCLI.class );

    private GigaSpacesTemplate template;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GigaSpaces Master ... \n" );
        ExpressionExperimentMasterCLI p = new ExpressionExperimentMasterCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( this.getClass().getName(), args );
        try {
            init();
            start();
        } catch ( Exception e ) {
            log.error( "Transformation error..." + e.getMessage() );
            e.printStackTrace();
        }

        return err;
    }

    /**
     * @throws Exception
     */
    protected void init() throws Exception {
        template = ( GigaSpacesTemplate ) this.getBean( "gigaspacesTemplate" );
    }

    /**
     * 
     *
     */
    protected void start() {
        ExpressionExperimentTask proxy = ( ExpressionExperimentTask ) this.getBean( "proxy" );
        for ( int i = 0; i < 2; i++ ) {
            Stopwatch stopwatch = new Stopwatch();
            stopwatch.start();

            // TODO remove me - using this test expression experiment
            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
            long time = stopwatch.getElapsedTime();
            ee.setName( "Expression Experiment from " + this.getClass().getName() + " " + time );
            ee.setShortName( "Expression Experiment " + time );
            // Foo foo = new Foo();
            // foo.setAString( "String from " + this.getClass().getName() );
            // Result res = proxy.execute( foo );

            Result res = proxy.execute( ee );
            long wt = stopwatch.stop().getElapsedTime();
            log.info( "Submitted Job " + res.getTaskID() + " in " + wt + " ms.  Result expression experiment id is "
                    + res.getAnswer() );

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }
}
