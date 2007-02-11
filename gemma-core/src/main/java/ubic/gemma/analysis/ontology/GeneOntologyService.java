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
package ubic.gemma.analysis.ontology;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.basecode.dataStructure.graph.DirectedGraph;
import ubic.basecode.dataStructure.graph.DirectedGraphNode;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.description.OntologyEntryService;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup. Where possible, use this instead of calling
 * OntologyEntryService.getChildren etc.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geneOntologyService"
 * @spring.property name="ontologyEntryService" ref="ontologyEntryService"
 */
public class GeneOntologyService implements InitializingBean {

    private static Log log = LogFactory.getLog( GeneOntologyService.class.getName() );

    private OntologyEntryService ontologyEntryService;

    private DirectedGraph graph = null;

    private AtomicBoolean ready = new AtomicBoolean( false );

    private AtomicBoolean running = new AtomicBoolean( false );

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        // FIXME: this should look for GeneOntology specifically; this is not guaranteed by this.

        if ( running.get() ) {
            log.warn( "GO initialization is already running" );
            return;
        }

        final OntologyEntry root = ontologyEntryService.findByAccession( "all" );
        if ( root == null ) {
            log.warn( "Could not locate root of GO" );
            return;
        }
        if ( !root.getExternalDatabase().getName().equals( "GO" ) ) {
            log.warn( "Could not locate root of GO (got something else instead: " + root + ")" );
            return;
        }

        graph = new DirectedGraph();
        Thread loadThread = new Thread( new Runnable() {
            public void run() {
                if ( running.get() ) return;
                running.set( true );
                log.info( "Loading Gene Ontology..." );
                try {
                    graph.addNode( new DirectedGraphNode( root, root, graph ) );
                    addChildrenOf( root );
                } catch ( Exception e ) {
                    log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
                ready.set( true );
                running.set( false );
                log.info( "Gene Ontology loaded" );
            }
        } );

        loadThread.start();

    }

    @SuppressWarnings("unchecked")
    private void addChildrenOf( OntologyEntry item ) {
        if ( item == null ) return;

        Collection<OntologyEntry> children = ontologyEntryService.getChildren( item );
        if ( Math.random() < 0.005 && children.size() > 0 ) { // report only occasional updates.
            log.info( "Loading " + children.size() + " children of " + item + " (among others)..." );
        }
        if ( children == null ) return;
        for ( OntologyEntry entry : children ) {
            if ( entry == null ) continue;
            graph.addChildTo( item, entry, entry );
            addChildrenOf( entry );
        }
    }

    /**
     * Return the immediate parent(s) of the given entry.
     * 
     * @param entry
     * @return collection, because entries can have multiple parents.
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyEntry> getParents( OntologyEntry entry ) {
        if ( !ready.get() ) {
            return ontologyEntryService.getParents( entry );
        }
        Collection<DirectedGraphNode> parents = ( ( DirectedGraphNode ) graph.get( entry ) ).getParentNodes();
        Collection<OntologyEntry> returnVal = new HashSet<OntologyEntry>();
        for ( DirectedGraphNode node : parents ) {
            if ( node == null ) continue;
            OntologyEntry goEntry = ( OntologyEntry ) node.getItem();
            if ( goEntry == null ) continue;
            returnVal.add( goEntry );
        }
        return returnVal;
    }

    /**
     * Returns the immediate children of the given entry
     * 
     * @param entry
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyEntry> getChildren( OntologyEntry entry ) {
        if ( !ready.get() ) {
            return ontologyEntryService.getChildren( entry );
        }
        Collection<DirectedGraphNode> children = ( ( DirectedGraphNode ) graph.get( entry ) ).getChildNodes();
        Collection<OntologyEntry> returnVal = new HashSet<OntologyEntry>();
        for ( DirectedGraphNode node : children ) {
            if ( node == null ) continue;
            OntologyEntry goEntry = ( OntologyEntry ) node.getItem();
            if ( goEntry == null ) continue;
            returnVal.add( goEntry );
        }
        return returnVal;
    }

    public void setOntologyEntryService( OntologyEntryService ontologyEntryService ) {
        this.ontologyEntryService = ontologyEntryService;
    }

}
