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
package ubic.gemma.grid.javaspaces.analysis.sequence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI;
import ubic.gemma.grid.javaspaces.CustomDelegatingWorker;
import ubic.gemma.util.SecurityUtil;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignRepeatScanSpacesWorkerCLI extends AbstractSpacesWorkerCLI {

    private static Log log = LogFactory.getLog( ArrayDesignRepeatScanSpacesWorkerCLI.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#setRegistrationEntryTask()
     */
    @Override
    protected void setRegistrationEntryTask() throws Exception {
        registrationEntry.message = ArrayDesignRepeatScanTask.class.getName();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#setWorker()
     */
    @Override
    protected void setWorker() {
        worker = ( CustomDelegatingWorker ) updatedContext.getBean( "arrayDesignRepeatScanWorker" );
    }

    /*
     * (non-Javadoc)
     * 
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
        log.info( "Starting spaces worker to run array design repeat scan ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        ArrayDesignRepeatScanSpacesWorkerCLI p = new ArrayDesignRepeatScanSpacesWorkerCLI();
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
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

}
