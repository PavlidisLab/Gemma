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

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import org.springmodules.javaspaces.DelegatingWorker;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.javaspaces.GemmaSpacesRegistrationEntry;

import com.j_spaces.core.IJSpace;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractGemmaSpacesWorkerCLI extends AbstractSpringAwareCLI implements RemoteEventListener {

    protected GigaSpacesTemplate template;

    protected DelegatingWorker worker;

    protected Thread itbThread;

    protected IJSpace space = null;

    protected GemmaSpacesRegistrationEntry genericEntry = null;

    protected Long workerRegistrationId = null;

    /**
     * Initializes the spring beans.
     * 
     * @throws Exception
     */
    protected abstract void init() throws Exception;

    /**
     * Starts the thread for this worker.
     */
    protected abstract void start();

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
            log.error( "transError problem..." + e.getMessage() );
            e.printStackTrace();
        }
        return err;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        // TODO Auto-generated method stub

    }

    /**
     * A worker shutdown hook.
     * 
     * @author keshav
     */
    public class ShutdownHook extends Thread {
        // TODO move me to a base task.
        public void run() {
            log.info( "Worker shut down.  Running shutdown hook ... cleaning up registered entries for this worker." );
            if ( space != null ) {
                try {
                    space.clear( genericEntry, null );
                } catch ( Exception e ) {

                    log.error( "Error clearing the generic entry " + genericEntry + "for task " + genericEntry.message
                            + "from space." );
                    e.printStackTrace();
                }
            }
        }
    }
}
