/*
 * The Gemma projec
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
package ubic.gemma.core.job.executor.worker;

import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.AbstractSpringAwareCLI;
import ubic.gemma.persistence.util.SpringContextUtil;

/**
 * Generic tool for starting a remote worker.
 *
 * @author keshav
 */
public class WorkerCLI extends AbstractSpringAwareCLI {

    private RemoteTaskRunningService taskRunningService;

    public static void main( String[] args ) {
        WorkerCLI me = new WorkerCLI();
        me.doWork( args );
    }

    @Override
    public String getShortDesc() {
        return "Start worker application for processing tasks sent by Gemma webapp.";
    }

    @Override
    protected void buildOptions() {
        // Option mmtxOption = OptionBuilder.withDescription( "Set to force MMTX to be initialized" ).create( "mmtx" );
        // super.addOption( mmtxOption );
    }

    @Override
    protected void createSpringContext() {
        ctx = SpringContextUtil.getApplicationContext( hasOption( "testing" ), false /* webapp */,
                getAdditionalSpringConfigLocations() );

        /*
         * Important to ensure that threads get permissions from their context - not global!
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception commandArgumentErrors = processCommandLine( args );
        if ( commandArgumentErrors != null ) {
            return commandArgumentErrors;
        }

        try {
            init();
        } catch ( Exception e ) {
            log.error( e, e );
        }
        return commandArgumentErrors;
    }

    @Override
    protected String[] getAdditionalSpringConfigLocations() {
        String[] workerSpecificConfigs = { "classpath*:ubic/gemma/workerContext-component-scan.xml",
                "classpath*:ubic/gemma/workerContext-jms.xml" };
        return workerSpecificConfigs;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        // FIXME
        // if ( this.hasOption( "mmtx" ) ) {
        // ExpressionExperimentAnnotator eeAnnotator = this.getBean( ExpressionExperimentAnnotator.class );
        // eeAnnotator.init();
        // }
    }

    private void init() throws Exception {
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook( shutdownHook );

        taskRunningService = ctx.getBean( RemoteTaskRunningService.class );
    }

    @Override
    public String getCommandName() {
        return "startWorker";
    }

    public class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info( "Remote task executor is shutting down..." );
            log.info( "Attempting to cancel all running tasks..." );
            taskRunningService.shutdown();
            log.info( "Shutdown sequence completed." );
        }
    }
}
