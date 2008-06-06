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

package ubic.gemma.ontology;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.larq.IndexLARQ;

/**
 * @author kelsey
 * @version $Id$
 */
public abstract class AbstractOntologyService {

    protected static final Log log = LogFactory.getLog( AbstractOntologyService.class );

    protected Map<String, OntologyTerm> terms;
    protected Map<String, OntologyIndividual> individuals;

    private boolean enabled = false;

    protected AtomicBoolean ready = new AtomicBoolean( false );

    protected AtomicBoolean modelReady = new AtomicBoolean( false );
    protected AtomicBoolean indexReady = new AtomicBoolean( false );
    protected AtomicBoolean cacheReady = new AtomicBoolean( false );

    protected AtomicBoolean running = new AtomicBoolean( false );
    protected String ontology_URL;
    protected String ontologyName;
    protected OntModel model;
    protected IndexLARQ index;

    /**
     * Delegates the call as to load the model into memory or leave it on disk. Simply delegates to either
     * OntologyLoader.loadMemoryModel( url ); OR OntologyLoader.loadPersistentModel( url, spec );
     * 
     * @param url
     * @return
     * @throws IOException
     */
    protected abstract OntModel loadModel( String url );

    /**
     * Defines the location of the ontology eg: http://mged.sourceforge.net/ontologies/MGEDOntology.owl
     * 
     * @return
     */
    protected abstract String getOntologyUrl();

    /**
     * The simple name of the ontology. Used for indexing purposes. (ie this will determine the name of the underlying
     * index for searching the ontology)
     * 
     * @return
     */
    protected abstract String getOntologyName();

    public AbstractOntologyService() {
        super();
        ontology_URL = getOntologyUrl();
        ontologyName = getOntologyName();
    }

    /**
     * Looks for a OntologyTerm that has the matchine URI given
     * 
     * @param uri
     * @return
     */
    public OntologyTerm getTerm( String uri ) {

        if ( ( uri == null ) || ( !ready.get() ) ) return null;

        OntologyTerm term = terms.get( uri );

        return term;
    }

    /**
     * Looks through both Terms and Individuls for a OntologyResource that has a uri matching the uri given If no
     * OntologyTerm is found only then will ontologyIndividuals be searched. returns null if nothing is found.
     * 
     * @param uri
     * @return
     */
    public OntologyResource getResource( String uri ) {

        if ( ( uri == null ) || ( !ready.get() ) ) return null;

        OntologyResource resource = terms.get( uri );

        if ( resource == null ) resource = individuals.get( uri );

        return resource;
    }

    /**
     * @param uri
     * @return
     */
    public Collection<OntologyIndividual> getTermIndividuals( String uri ) {
        OntologyTerm term = terms.get( uri );
        if ( term == null ) {
            /*
             * Either the onology hasn't been loaded, or the id was not valid.
             */
            throw new IllegalArgumentException( "No term for URI=" + uri + " in " + this.getOntologyName()
                    + "; make sure ontology is loaded and uri is valid" );
        }
        return term.getIndividuals( true );

    }

    /**
     * Looks for any ontologyTerms that match the given search string
     * 
     * @param search
     * @return
     */
    public Collection<OntologyTerm> findTerm( String search ) {

        if ( !isOntologyLoaded() ) return null;

        log.debug( "Searching " + this.getOntologyName() + " for '" + search + "'" );

        assert index != null : "attempt to search " + this.getOntologyName() + " when index is null";
        Collection<OntologyTerm> matches = OntologySearch.matchClasses( model, index, search );
        return matches;
    }

    /**
     * Looks for any OntologyIndividuals or ontologyTerms that match the given search string
     * 
     * @param search
     * @return results, or an empty collection if the results are empty OR the ontology is not available to be searched.
     */
    public Collection<OntologyResource> findResources( String search ) {

        if ( !isOntologyLoaded() ) return new HashSet<OntologyResource>();

        assert index != null : "attempt to search " + this.getOntologyName() + " when index is null";

        Collection<OntologyResource> res = OntologySearch.matchResources( model, index, search.trim() + "*" );

        return res;
    }

    /**
     * Looks for any OntologyIndividuals that match the given search string
     * 
     * @param search
     * @return
     */
    public Collection<OntologyIndividual> findIndividuals( String search ) {

        if ( !isOntologyLoaded() ) return null;

        assert index != null : "attempt to search " + this.getOntologyName() + " when index is null";
        // if ( index == null ) index = OntologyIndexer.indexOntology( ontology_name, model );

        Collection<OntologyIndividual> indis = OntologySearch.matchIndividuals( model, index, search );

        return indis;
    }

    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            log.warn( ontology_URL + " initialization is already running" );
            return;
        }

        if ( this.isOntologyLoaded() ) {
            return;
        }

        String configParameter = "load." + ontologyName;
        boolean loadOntology = ConfigUtils.getBoolean( configParameter, true );

        // if loading ontologies is disabled in the configuration, return
        if ( !force && !loadOntology ) {
            log.info( "Loading " + ontologyName + " is disabled (force=" + force + ", " + configParameter + "="
                    + loadOntology + ")" );
            return;
        }

        enabled = true;

        // Load the model for searching

        Thread loadThread = new Thread( new Runnable() {
            public void run() {

                running.set( true );

                terms = new HashMap<String, OntologyTerm>();
                individuals = new HashMap<String, OntologyIndividual>();

                log.info( "Loading " + ontologyName + " Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();

                boolean interrupted = false;
                int waitMs = 1000;
                while ( !interrupted && !modelReady.get() ) {
                    try {
                        /*
                         * We use the OWL_MEM_TRANS_INF spec so we can do 'getChildren' and get _all_ the children in
                         * one query.
                         */
                        model = loadModel( ontology_URL );
                        modelReady.set( true );
                    } catch ( Exception e ) {
                        log.error( "error loading model for " + ontologyName, e );
                        try {
                            log.error( "waiting " + waitMs + "ms before trying to reload" );
                            Thread.sleep( waitMs );
                            waitMs *= 2;
                        } catch ( InterruptedException ie ) {
                            interrupted = true;
                        }
                    }
                }

                try {

                    /*
                     * Indexing will be slow the first time (can take hours for large ontologies).
                     */
                    log.info( "Loading Index for " + ontologyName + " Ontology" );
                    index = OntologyIndexer.indexOntology( ontologyName, model );
                    log.info( "Done Loading Index for " + ontologyName + " Ontology in " + loadTime.getTime() / 1000
                            + "s" );
                    indexReady.set( true );

                    /*
                     * This creates a cache of URI (String) --> OntologyTerms. ?? Does Jena provide an easier way to do
                     * this?
                     */
                    loadTermsInNameSpace( ontology_URL );
                    log.info( ontology_URL + "  loaded, total of " + terms.size() + " items in " + loadTime.getTime()
                            / 1000 + "s" );
                    cacheReady.set( true );

                    ready.set( true );
                    running.set( false );
                    loadTime.stop();

                    log.info( "Finished loading ontology " + ontologyName + " in " + loadTime.getTime() / 1000 + "s" );

                } catch ( Exception e ) {
                    log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        }, this.ontologyName + "_load_thread" );

        synchronized ( running ) {
            if ( running.get() ) return;
            loadThread.start();
        }

        startKeepAliveThread();
    }

    /**
     * @param url
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) {
        Collection<OntologyResource> terms = OntologyLoader.initialize( url, model );
        addTerms( terms );
    }

    /**
     * @param newTerms
     */
    private void addTerms( Collection<OntologyResource> newTerms ) {
        if ( terms == null ) terms = new HashMap<String, OntologyTerm>();
        if ( individuals == null ) individuals = new HashMap<String, OntologyIndividual>();

        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null ) continue;
            if ( term instanceof OntologyTerm ) terms.put( term.getUri(), ( OntologyTerm ) term );
            if ( term instanceof OntologyIndividual ) individuals.put( term.getUri(), ( OntologyIndividual ) term );
        }
    }

    /**
     * Used for determining if the Gene Ontology has finished loading into memory yet Although calls like getParents,
     * getChildren will still work (its much faster once the ontologies have been preloaded into memory.)
     * 
     * @returns boolean
     */
    public synchronized boolean isOntologyLoaded() {
        return ready.get();
    }

    /*
     * a collection of the keep-alive threads for each concrete subclass; a single variable here wouldn't work because
     * all of the subclasses would be using the same variable.
     */
    private static Map<Class, Thread> keepAliveThreads = Collections.synchronizedMap( new HashMap<Class, Thread>() );

    /*
     * the number of milliseconds between keep-alive queries; this should be less than or equal to MySQL wait_timeout
     * server variable.  Currently 4 hours.  (half of wait_timeout)
     */
    private static final int KEEPALIVE_PING_DELAY = 14400 * 1000;   

    /*
     * a term to search for; matters not at all...
     */
    private static final String KEEPALIVE_SEARCH_TERM = "dummy";

    private synchronized void startKeepAliveThread() {
        if ( keepAliveThreads.containsKey( this.getClass() ) ) return;
        Thread keepAliveThread = new KeepAliveThread();
        keepAliveThread.start();
        keepAliveThreads.put( this.getClass(), keepAliveThread );
    }

    private class KeepAliveThread extends Thread {
        public void run() {
            for ( ;; ) {
                try {
                    Thread.sleep( KEEPALIVE_PING_DELAY );
                } catch ( InterruptedException e ) {
                }
                if ( isOntologyLoaded() ) {
                    log.info( "sending keep-alive query to " + getOntologyName() );
                    try {
                        findResources( KEEPALIVE_SEARCH_TERM );
                    } catch ( Exception e ) {
                        log.error( "error sending keep-alive query to " + getOntologyName(), e );
                    }
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Use this to turn this ontology on or off.
     * 
     * @param enabled If false, the ontology will not be loaded.
     */
    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }
}