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

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springmodules.javaspaces.DelegatingWorker;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.SecurityUtil;
import ubic.gemma.util.javaspaces.gigaspaces.GigaspacesUtil;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentWorkerCLI extends AbstractSpringAwareCLI {

    private static Log log = LogFactory.getLog( ExpressionExperimentWorkerCLI.class );

    // member for gigaspaces template
    private GigaSpacesTemplate template;
    // The delegator worker
    private DelegatingWorker iTestBeanWorker;

    private Thread itbThread;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    protected void init() throws Exception {

        GigaspacesUtil gigaspacesUtil = ( GigaspacesUtil ) this.getBean( "gigaspacesUtil" );
        ApplicationContext updatedContext = gigaspacesUtil
                .addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) )
            throw new RuntimeException( "Gigaspaces beans could not be loaded. Cannot start worker." );

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );
        iTestBeanWorker = ( DelegatingWorker ) updatedContext.getBean( "testBeanWorker" );
    }

    protected void start() {
        log.debug( "Current Thread: " + Thread.currentThread().getName() + " Authentication: "
                + SecurityContextHolder.getContext().getAuthentication() );

        itbThread = new Thread( iTestBeanWorker );
        itbThread.start();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GigaSpaces Worker To Handle Expression Experiments ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        ExpressionExperimentWorkerCLI p = new ExpressionExperimentWorkerCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( this.getClass().getName(), args );
        try {
            init();
            start();
        } catch ( Exception e ) {
            log.error( "transError problem..." + e.getMessage() );
            e.printStackTrace();
        }
        return err;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

}
