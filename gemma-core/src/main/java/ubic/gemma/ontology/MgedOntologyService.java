/*
 * The GemmaOnt project
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.model.common.description.VocabCharacteristicImpl;
import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModelSpec;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 * 
 * @author klc
 * @version $Id: MgedOntologyService.java
 * @spring.bean id="mgedOntologyService"
 */

public class MgedOntologyService implements InitializingBean {

    protected static final Log log = LogFactory.getLog( MgedOntologyService.class );

    // map of uris to terms
    private static Map<String, OntologyTerm> terms;

    private static final AtomicBoolean ready = new AtomicBoolean( false );

    private static final AtomicBoolean running = new AtomicBoolean( false );

    //private static final String BASE_MGED_URI = "http://purl.org/obo/owl/GO#";
    private final static String MGED_URL = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl";

    public void afterPropertiesSet() throws Exception {
        log.debug( "entering AfterpropertiesSet" );
        if ( running.get() ) {
            log.warn( "MGED initialization is already running" );
            return;
        }
        init();
    }

    public OntologyTerm getTerm( String id ){

        OntologyTerm term = terms.get( id );
     
        return term;
    }
    
    public void saveStatement(VocabCharacteristicImpl vc, Collection<Long> bioMaterialIdList){
        

        log.info( "Vocab Characteristic: " + vc );
        log.info( "Biomaterial ID List: " + bioMaterialIdList );
    }
    
    public Collection<OntologyTreeNode> getBioMaterialTerms() {

        Collection<OntologyTreeNode> nodes = new ArrayList<OntologyTreeNode>();

        final String id = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#MGEDOntology";
        OntologyTerm term = terms.get( id );

        nodes.add( buildTreeNode( term ) );
        return nodes;
    }

    public Collection<OntologyRestriction> getTermRestrictions( String id ) {

        OntologyTerm term = terms.get( id );

        return term.getRestrictions();

    }

    public Collection<OntologyIndividual> getTermIndividuals( String id ) {

        OntologyTerm term = terms.get( id );

        return term.getIndividuals( true );

    }

    /**
     * @param node Recursivly builds the tree node structure that is needed by the ext tree
     */
    protected OntologyTreeNode buildTreeNode( OntologyTerm term ) {

        OntologyTreeNode node = new OntologyTreeNode( term );
        node.setLeaf( true );
        Collection<OntologyTerm> children = term.getChildren( true );

        if ( ( children != null ) && ( !children.isEmpty() ) ) {
            // node has children
            node.setAllowChildren( true );
            node.setLeaf( false );

            for ( OntologyTerm child : children ) {
                node.appendChild( buildTreeNode( child ) );
            }
        }

        return node;

    }

    protected synchronized void init() {

        boolean loadOntology = ConfigUtils.getBoolean( "loadOntology", true );

        // if loading ontologies is disabled in the configuration, return
        if ( !loadOntology ) {
            log.info( "Loading Mged is disabled" );
            return;
        }

        Thread loadThread = new Thread( new Runnable() {
            public void run() {

                running.set( true );
                terms = new HashMap<String, OntologyTerm>();
                log.info( "Loading mged Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {
                    loadTermsInNameSpace( MGED_URL );
                    log.info( "MGED Ontology loaded, total of " + terms.size() + " items in " + loadTime.getTime()
                            / 1000 + "s" );

                    ready.set( true );
                    running.set( false );

                    log.info( "Done loading MGED Ontology" );
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
        Collection<OntologyResource> terms = OntologyLoader.loadMemoryModel( url, OntModelSpec.OWL_MEM );
        addTerms( terms );
    }

    /**
     * Primarily here for testing.
     * 
     * @param is
     * @throws IOException
     */
    protected void loadTermsInNameSpace( InputStream is ) throws IOException {
        Collection<OntologyResource> terms = OntologyLoader.loadMemoryModel( is, null, OntModelSpec.OWL_MEM );
        addTerms( terms );
    }

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
    public synchronized boolean isGeneOntologyLoaded() {

        return ready.get();
    }

}
