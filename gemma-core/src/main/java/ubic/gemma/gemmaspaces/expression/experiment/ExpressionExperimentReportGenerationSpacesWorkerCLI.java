/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.gemmaspaces.expression.experiment;

import net.jini.space.JavaSpace;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.gemmaspaces.AbstractGemmaSpacesWorkerCLI;
import ubic.gemma.gemmaspaces.CustomDelegatingWorker;
import ubic.gemma.util.SecurityUtil;

/**
 * This command line interface is used to take {@link ExpressionExperimentReportService} tasks from the
 * {@link JavaSpace} and returns the results.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentReportGenerationSpacesWorkerCLI extends AbstractGemmaSpacesWorkerCLI {

    private static Log log = LogFactory.getLog( ExpressionExperimentReportGenerationSpacesWorkerCLI.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.gemmaspaces.AbstractGemmaSpacesWorkerCLI#setRegistrationEntryTask()
     */
    @Override
    protected void setRegistrationEntryTask() throws Exception {
        registrationEntry.message = ExpressionExperimentReportService.class.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.gemmaspaces.AbstractGemmaSpacesWorkerCLI#setWorker()
     */
    @Override
    protected void setWorker() {
        worker = ( CustomDelegatingWorker ) updatedContext.getBean( "expressionExperimentReportGenerationWorker" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.gemmaspaces.AbstractGemmaSpacesWorkerCLI#start()
     */
    @Override
    protected void start() {
        log.debug( "Authentication: " + SecurityContextHolder.getContext().getAuthentication() );

        itbThread = new Thread( worker );
        itbThread.start();

        log.info( this.getClass().getSimpleName() + " started successfully." );
    }

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
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running spaces worker to generate expression experiment reports ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        ExpressionExperimentReportGenerationSpacesWorkerCLI p = new ExpressionExperimentReportGenerationSpacesWorkerCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
