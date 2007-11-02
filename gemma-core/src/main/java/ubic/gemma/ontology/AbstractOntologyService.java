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

/**
 * @author kelsey
 * @version $Id$
 */
public abstract class AbstractOntologyService implements InitializingBean {

    protected static final Log log = LogFactory.getLog( AbstractOntologyService.class );

    protected Map<String, OntologyTerm> terms;
    protected Map<String, OntologyIndividual> individuals;
    protected AtomicBoolean ready = new AtomicBoolean( false );
    protected AtomicBoolean running = new AtomicBoolean( false );
    protected String ontology_URL;
    protected String ontology_name;
    protected OntModel model;
    protected IndexLARQ index;

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
        ontology_name = getOntologyName();
    }

    public void afterPropertiesSet() throws Exception {
        log.debug( "entering AfterpropertiesSet" );
        if ( running.get() ) {
            log.warn( ontology_URL + " initialization is already running" );
            return;
        }
        init();
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
     * Looks for a OntologyIndividual that has the matcing URI given
     * 
     * @param uri
     * @return
     */
    public OntologyIndividual getIndividual( String uri ) {

        if ( ( uri == null ) || ( !ready.get() ) ) return null;

        OntologyIndividual indi = individuals.get( uri );

        return indi;
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
     * 
     * @param uri
     * @return
     */
    public Collection<OntologyRestriction> getTermRestrictions( String uri ) {

        OntologyTerm term = terms.get( uri );
        if ( term == null ) {
            /*
             * Either the onology hasn't been loaded, or the id was not valid.
             */
            throw new IllegalArgumentException( "No term for URI=" + uri + " in " + this.getOntologyName()
                    + "; make sure ontology is loaded and uri is valid" );
        }
        return term.getRestrictions();

    }

    /**
     * 
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

        assert index != null : "attempt to search " + this.getOntologyName() + " when index is null";
        // if ( index == null ) index = OntologyIndexer.indexOntology( ontology_name, model );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, search );

        return name;
    }

    /**
     * Looks for any OntologyIndividuals or ontologyTerms that match the given search string
     * 
     * @param search
     * @return
     */
    public Collection<OntologyResource> findResources( String search ) {

        if ( !isOntologyLoaded() ) return null;

        assert index != null : "attempt to search " + this.getOntologyName() + " when index is null";
        // if ( index == null ) index = OntologyIndexer.indexOntology( ontology_name, model );

        Collection<OntologyResource> res = OntologySearch.matchResources( model, index, search );

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

    protected synchronized void init() {

        boolean loadOntology = ConfigUtils.getBoolean( "load." + ontology_name, true );

        // if loading ontologies is disabled in the configuration, return
        if ( !loadOntology ) {
            log.info( "Loading " + ontology_name + " is disabled" );
            return;
        }

        // Load the model for searching

        Thread loadThread = new Thread( new Runnable() {
            public void run() {

                running.set( true );

                terms = new HashMap<String, OntologyTerm>();
                individuals = new HashMap<String, OntologyIndividual>();

                log.debug( "Loading " + ontology_name + " Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();

                try {

                    model = loadModel( ontology_URL, OntModelSpec.OWL_MEM );

                    loadTermsInNameSpace( ontology_URL );
                    log.debug( ontology_URL + "  loaded, total of " + terms.size() + " items in " + loadTime.getTime()
                            / 1000 + "s" );

                    log.info( "Loading Index for " + ontology_name + " Ontology" );
                    index = OntologyIndexer.indexOntology( ontology_name, model );
                    log.info( "Done Loading Index for " + ontology_name + " Ontology in " + loadTime.getTime() / 1000
                            + "s" );

                    ready.set( true );
                    running.set( false );
                    loadTime.stop();

                    log.info( "Finished loading ontology " + ontology_name + " in " + loadTime.getTime() / 1000 + "s" );

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
     * getChildren will still work (its much faster once the gene ontologies have been preloaded into memory.
     * 
     * @returns boolean
     */
    public synchronized boolean isOntologyLoaded() {

        return ready.get();
    }

}