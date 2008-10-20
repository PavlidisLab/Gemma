/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.grid.javaspaces.worker;

import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI;
import ubic.gemma.grid.javaspaces.CustomDelegatingWorker;
import ubic.gemma.grid.javaspaces.analysis.coexpression.links.LinkAnalysisTask;
import ubic.gemma.util.SecurityUtil;

/**
 * @author keshav
 * @version $Id$
 */
public class LinkAnalysisSpacesWorker extends AbstractSpacesWorkerCLI {

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#setRegistrationEntryTask()
     */
    @Override
    protected void setRegistrationEntryTask() throws Exception {
        registrationEntry.message = LinkAnalysisTask.class.getName();

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#setWorker()
     */
    @Override
    protected void setWorker() {
        worker = ( CustomDelegatingWorker ) updatedContext.getBean( "linkAnalysisWorker" );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#start()
     */
    @Override
    protected void start() {
        log.debug( "Authentication: " + SecurityContextHolder.getContext().getAuthentication() );

        itbThread = new Thread( worker );
        itbThread.start();

        log.info( this.getClass().getSimpleName() + " started successfully." );
    }

    /**
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Starting spaces worker to run link analysis ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        LinkAnalysisSpacesWorker p = new LinkAnalysisSpacesWorker();
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
