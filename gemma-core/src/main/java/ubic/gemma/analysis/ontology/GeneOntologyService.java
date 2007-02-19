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
import ubic.gemma.util.ConfigUtils;

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

    /**
     * Report only this fraction (on average) of term loading events.
     */
    private static final double FRACTION_OF_LOAD_UPDATE_LOGGING = 0.0002;

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

        init();

    }

    /**
     * This is made protected so it can be tested.
     */
    protected void init() {
        boolean loadOntology = ConfigUtils.getBoolean( "loadOntology", true );
        // if loading ontologies is disabled in the configuration, return
        if ( !loadOntology ) {
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
                ready.set( false );
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
        if ( Math.random() < FRACTION_OF_LOAD_UPDATE_LOGGING && children.size() > 0 ) { // report only occasional
                                                                                        // updates.
            log.info( "Loading " + children.size() + " children of " + item + " (among others)..." );
        }
        if ( children == null || children.size() == 0 ) {
            log.debug( item + " has no children" );
            return;
        }
        for ( OntologyEntry entry : children ) {
            log.debug( "Adding " + entry );
            graph.addChildTo( item, entry, entry );
            addChildrenOf( entry );
        }
    }

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     * 
     * @param entry
     * @return collection, because entries can have multiple parents. (root is excluded)
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyEntry> getParents( OntologyEntry entry ) {
        if ( !ready.get() ) {
            return ontologyEntryService.getParents( entry );
        }

        DirectedGraphNode childNode = ( DirectedGraphNode ) graph.get( entry );
        if ( childNode == null ) {
            log.warn( "GO does not contain " + entry );
            return null;
        }

        Collection<DirectedGraphNode> parents = childNode.getParentNodes();

        Collection<OntologyEntry> returnVal = new HashSet<OntologyEntry>();
        for ( DirectedGraphNode node : parents ) {
            if ( node == null ) continue;
            OntologyEntry goEntry = ( OntologyEntry ) node.getItem();
            if ( isRoot( goEntry ) ) continue;
            if ( goEntry == null ) continue;
            returnVal.add( goEntry );
        }
        return returnVal;
    }

    /**
     * Return all the parents of an OntologyEntry, up to the root. NOTE: the term itself is NOT included; nor is the
     * root.
     * 
     * @param entry
     * @return parents (excluding the root)
     */
    public Collection<OntologyEntry> getAllParents( OntologyEntry entry ) {
        Collection<OntologyEntry> result = new HashSet<OntologyEntry>();
        getAllParents( entry, result );
        return result;
    }

    /**
     * @param entry
     * @param parents (excluding the root)
     */
    private void getAllParents( OntologyEntry entry, Collection<OntologyEntry> parents ) {
        if ( parents == null ) throw new IllegalArgumentException();
        Collection<OntologyEntry> immediateParents = getParents( entry );
        if ( immediateParents == null ) return;
        for ( OntologyEntry entry2 : immediateParents ) {
            if ( isRoot( entry2 ) ) continue;
            parents.add( entry2 );
            getAllParents( entry2, parents );
        }
    }

    /**
     * @param entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *        included incidentally if they are parents of other terms in the collection.
     * @return
     */
    public Collection<OntologyEntry> getAllParents( Collection<OntologyEntry> entries ) {
        if ( entries == null ) return null;
        Collection<OntologyEntry> result = new HashSet<OntologyEntry>();
        for ( OntologyEntry entry : entries ) {
            getAllParents( entry, result );
        }
        return result;
    }

    /**
     * @param entry
     * @return
     */
    public Collection<OntologyEntry> getAllChildren( OntologyEntry entry ) {
        Collection<OntologyEntry> result = new HashSet<OntologyEntry>();
        getAllChildren( entry, result );
        return result;
    }

    /**
     * @param entry
     * @param children
     */
    private void getAllChildren( OntologyEntry entry, Collection<OntologyEntry> children ) {
        if ( children == null ) throw new IllegalArgumentException();
        Collection<OntologyEntry> immediateChildren = getChildren( entry );
        if ( immediateChildren == null ) return;
        for ( OntologyEntry entry2 : immediateChildren ) {
            children.add( entry2 );
            getAllChildren( entry2, children );
        }
    }

    /**
     * Determines if one ontology entry is a parent (direct or otherwise) of a given child term.
     * 
     * @param child
     * @param potentialParent
     * @return True if potentialParent is in the parent graph of the child; false otherwise.
     */
    public Boolean isAParentOf( OntologyEntry child, OntologyEntry potentialParent ) {
        if ( isRoot( potentialParent ) ) return true;
        Collection<OntologyEntry> parents = getAllParents( child );
        return parents.contains( potentialParent );
    }

    /**
     * @param entity
     * @return
     */
    protected Boolean isRoot( OntologyEntry entity ) {
        return entity.getAccession().equals( "all" ); // FIXME, use the line below instead.
        // return entity.equals( this.graph.getRoot().getItem() );
    }

    /**
     * Determins if one ontology entry is a child (direct or otherwise) of a given parent term.
     * 
     * @param parent
     * @param potentialChild
     * @return
     */
    public Boolean isAChildOf( OntologyEntry parent, OntologyEntry potentialChild ) {
        return isAParentOf( potentialChild, parent );
    }

    /**
     * Returns the immediate children of the given entry
     * 
     * @param entry
     * @return children of entry, or null if there are no children (or if entry is null)
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyEntry> getChildren( OntologyEntry entry ) {
        if ( entry == null ) return null;
        if ( !ready.get() ) {
            return ontologyEntryService.getChildren( entry );
        }
        DirectedGraphNode parentNode = ( DirectedGraphNode ) graph.get( entry );
        if ( parentNode == null ) return null;
        Collection<DirectedGraphNode> children = parentNode.getChildNodes();
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
