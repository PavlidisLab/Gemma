/*
 * The Gemma project.
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

package ubic.gemma.model.common.description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.description.OntologyEntryService

 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class OntologyEntryServiceImpl extends ubic.gemma.model.common.description.OntologyEntryServiceBase {

    private static Log log = LogFactory.getLog( OntologyEntryServiceImpl.class.getName() );

    /**
     * @see ubic.gemma.model.common.description.OntologyEntryService#findOrCreate(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected ubic.gemma.model.common.description.OntologyEntry handleFindOrCreate(
            ubic.gemma.model.common.description.OntologyEntry ontologyEntry ) throws java.lang.Exception {
        return this.getOntologyEntryDao().findOrCreate( ontologyEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.OntologyEntryService#remove(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.common.description.OntologyEntry ontologyEntry )
            throws java.lang.Exception {
        this.getOntologyEntryDao().remove( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleCreate(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected OntologyEntry handleCreate( OntologyEntry ontologyEntry ) throws Exception {
        return ( OntologyEntry ) this.getOntologyEntryDao().create( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleUpdate(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected void handleUpdate( OntologyEntry ontologyEntry ) throws Exception {
        this.getOntologyEntryDao().update( ontologyEntry );
    }

    @Override
    protected void handleThaw( OntologyEntry ontologyEntry ) throws Exception {
        this.getOntologyEntryDao().thaw( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected OntologyEntry handleLoad( Long id ) throws Exception {
        return ( OntologyEntry ) this.getOntologyEntryDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBasee#handleFind(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected OntologyEntry handleFind( OntologyEntry ontologyEntry ) throws Exception {
        return this.getOntologyEntryDao().find( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleGetAllChildren(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected Collection handleGetAllChildren( OntologyEntry ontologyEntry ) throws Exception {
        return this.getOntologyEntryDao().getAllChildren( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleGetParents(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected Collection handleGetParents( OntologyEntry ontologyEntry ) throws Exception {
        return this.getOntologyEntryDao().getParents( ontologyEntry );
    }

    /**
     * @param ontologyEntries
     * @return
     * @throws Exception
     */
    @Override
    protected Map handleGetParents( Collection ontologyEntries ) throws Exception {
        return this.getOntologyEntryDao().getParents( ontologyEntries );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleGetChildren(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected Collection handleGetChildren( OntologyEntry ontologyEntry ) throws Exception {
        return this.getOntologyEntryDao().getChildren( ontologyEntry );
    }

    protected ubic.gemma.model.common.description.OntologyEntry handleFindByAccession( java.lang.String accession )
            throws java.lang.Exception {

        return this.getOntologyEntryDao().findByAccession( accession );

    }

    final String ALL = "all";

    // Caches
    private Map<String, OntologyEntry> ontologyCache = new HashMap<String, OntologyEntry>();
    private Map<OntologyEntry, Collection> ontologyTreeCache = new HashMap<OntologyEntry, Collection>();

    protected Map handleGetAllParents( Collection children ) {

        if ( ( children == null ) || ( children.isEmpty() ) ) return null;

        Collection<OntologyEntry> notCached = new ArrayList<OntologyEntry>();
        Map<OntologyEntry, Collection> allParents = new HashMap<OntologyEntry, Collection>();

        // Check to see if the childrens parents are already chached.
        // Check: if a child is the root then done.
        // Make sublist of nonchaced children whose parents need retrieving.
        for ( Object obj : children ) {

            OntologyEntry child = ( OntologyEntry ) obj;
            // log.info( "Checking cache for ontology entries" );

            if ( ontologyTreeCache.containsKey( child ) )
                allParents.put( child, ontologyTreeCache.get( child ) );

            else if ( child.getAccession().equalsIgnoreCase( ALL ) )
                continue;

            else
                notCached.add( child );
        }

        // all children where already cached. Just return.
        if ( notCached.isEmpty() ) return allParents;

        // Retrive the 1st level of the non-cached childrens parents.
        Map<OntologyEntry, Collection> parents = this.getParents( notCached );

        // Now for each non-cached child, we have all the parents. Use recurison to get all the parents parents and so
        // on.Then flatten out the returned results and add to allParents.
        for ( OntologyEntry child : notCached ) {

            Map<OntologyEntry, Collection> foundParents = new HashMap<OntologyEntry, Collection>();

            foundParents.put( child, parents.get( child ) );

            Map<OntologyEntry, Collection> grandParents = this.getAllParents( parents.get( child ) );

            if ( ( grandParents == null ) || grandParents.isEmpty() ) {
                cache( foundParents );
                allParents.putAll( foundParents );
                continue;
            }

            Collection<OntologyEntry> flatParents = new HashSet<OntologyEntry>();

            for ( OntologyEntry parent : grandParents.keySet() )
                flatParents.addAll( grandParents.get( parent ) );

            foundParents.get( child ).addAll( flatParents );
            cache( foundParents );
            // log.info("Caching parent entries" );
            allParents.putAll( foundParents );

        }

        return allParents;
    }

    // Modifies passed in collection.
    private void cache( Map<OntologyEntry, Collection> toCache ) {

        Map<OntologyEntry, Collection> cached = new HashMap<OntologyEntry, Collection>();

        if ( ( toCache == null ) || ( toCache.isEmpty() ) ) return;

        for ( OntologyEntry oe : toCache.keySet() ) {

            Collection<OntologyEntry> parents = toCache.get( oe );
            Collection<OntologyEntry> cachedParents = new HashSet<OntologyEntry>();

            for ( OntologyEntry parent : parents ) {

                if ( ontologyCache.containsKey( parent.getAccession() ) )
                    cachedParents.add( ontologyCache.get( parent.getAccession() ) );
                else {
                    cachedParents.add( parent );
                    ontologyCache.put( parent.getAccession(), parent );
                }

            }

            if ( ontologyCache.containsKey( oe.getAccession() ) )
                cached.put( ontologyCache.get( oe.getAccession() ), cachedParents );
            else {
                cached.put( oe, cachedParents );
                ontologyCache.put( oe.getAccession(), oe );
            }

        }

        ontologyTreeCache.putAll( cached );
        toCache = cached;

        log.debug( "Size of Ontology Parents Cache: " + ontologyTreeCache.keySet().size() );
        log.debug( "Size of ontology object cache: " + ontologyCache.size() );

    }

}