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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.larq.IndexLARQ;

public abstract class AbstractOntologyService implements InitializingBean {

    protected static final Log log = LogFactory.getLog( AbstractOntologyService.class );
    
    //FIXME:  if the following variable aren't static they end up being null at runtime. 
    //I think, spring creates its own instance of this class, and the initializingBean also has its own instance
    //of this class but the one that gets used is the spring copy which then doesn't have any attributes set if 
    //the attributes are not static.  Ie the two instances can only share info if the attributes are static.
    protected static Map<String, OntologyTerm> terms;    
    protected static AtomicBoolean ready = new AtomicBoolean( false );
    private static AtomicBoolean running = new AtomicBoolean( false );
    private static String ontology_URL;
    protected static String ontology_startingPoint;
    private static OntModel model;
    private static IndexLARQ index;

    
    /**
     * Delegates the call as to load the model into memory or leave it on disk. Simply delegates to either
     * OntologyLoader.loadMemoryModel( url, spec ); OR OntologyLoader.loadPersistentModel( url, spec );
     * 
     * @param url
     * @param spec
     * @return
     * @throws IOException
     */
    protected abstract OntModel loadModel( String url, OntModelSpec spec ) throws IOException;
    
    /**
     * Defines the location of the ontology
     * eg: http://mged.sourceforge.net/ontologies/MGEDOntology.owl
     * @return
     */
    protected abstract String getOntologyUrl();
    
    /**
     * Defines the starting point of the given ontology
     * eg: "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioMaterialPackage"
     * @return
     */
    protected abstract String getOntologyStartingPoint();
    
    
    public AbstractOntologyService() {
        super();        
        ontology_URL = getOntologyUrl();
        ontology_startingPoint = getOntologyStartingPoint();        
    }
    

    public void afterPropertiesSet() throws Exception {
        log.debug( "entering AfterpropertiesSet" );
        if ( running.get() ) {
            log.warn( ontology_URL + " initialization is already running" );
            return;
        }
        init();
    }

    public OntologyTerm getTerm( String id ) {

        OntologyTerm term = terms.get( id );

        return term;
    }

    public void loadNewOntology( String ontologyURL, String startingPointURL ) {

        if ( running.get() ) return;

        AbstractOntologyService.ontology_startingPoint = startingPointURL;
        AbstractOntologyService.ontology_URL = ontologyURL;

        AbstractOntologyService.ready = new AtomicBoolean( false );
        AbstractOntologyService.running = new AtomicBoolean( false );

        init();

    }

    public Collection<OntologyRestriction> getTermRestrictions( String id ) {

        OntologyTerm term = terms.get( id );

        return term.getRestrictions();

    }

    public Collection<OntologyIndividual> getTermIndividuals( String id ) {

        OntologyTerm term = terms.get( id );

        return term.getIndividuals( true );

    }

    public Collection<OntologyTerm> findTerm( String search ) {

        if ( !AbstractOntologyService.ready.get() ) return null;

        if ( index == null ) index = OntologyIndexer.indexOntology( "mged", model );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, search );

        return name;
    }

  
    protected synchronized void init() {

        boolean loadOntology = ConfigUtils.getBoolean( "loadOntology", true );

        // if loading ontologies is disabled in the configuration, return
        if ( !loadOntology ) {
            log.info( "Loading Mged is disabled" );
            return;
        }

        // Load the model for searching

        Thread loadThread = new Thread( new Runnable() {
            public void run() {

                running.set( true );
                terms = new HashMap<String, OntologyTerm>();
                log.debug( "Loading " + ontology_URL + " Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {

                    model = loadModel( ontology_URL, OntModelSpec.OWL_MEM );

                    loadTermsInNameSpace( ontology_URL );
                    log.debug( ontology_URL + "  loaded, total of " + terms.size() + " items in " + loadTime.getTime()
                            / 1000 + "s" );

                    ready.set( true );
                    running.set( false );

                    log.info( "Done loading " + ontology_URL + " Ontology" );

                    loadTime.stop();
                } catch ( Exception e ) {
                    log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        } );

        synchronized ( running ) {
            if ( running.get() ) return;
            loadThread.start();
        }

    }

    /**
     * @param url
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) throws IOException {
        Collection<OntologyResource> terms = OntologyLoader.initialize( url, model );
        addTerms( terms );
    }

    /**
     * Primarily here for testing.
     * 
     * @param is
     * @throws IOException
     */
    private void addTerms( Collection<OntologyResource> newTerms ) {
        if ( terms == null ) terms = new HashMap<String, OntologyTerm>();
        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null ) continue;
            if ( term instanceof OntologyTerm ) terms.put( term.getUri(), ( OntologyTerm ) term );
        }
    }

    /**
     * Used for determining if the Gene Ontology has finished loading into memory yet Although calls like getParents,
     * getChildren will still work (its much faster once the gene ontologies have been preloaded into memory.
     * 
     * @returns boolean
     */
    public synchronized boolean isOntologyLoaded() {

        return ready.get();
    }

}