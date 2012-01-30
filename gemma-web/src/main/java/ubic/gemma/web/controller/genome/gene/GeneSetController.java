/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.web.controller.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.web.persistence.SessionListManager;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.security.SecurityService;

/**
 * Exposes GeneServices methods over ajax
 * 
 * @author kelsey
 * @version $Id$
 */
@Controller
@RequestMapping("/geneSet")
public class GeneSetController {

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private SecurityService securityService = null;

    @Autowired
    private SessionListManager sessionListManager;

    /**
     * AJAX create an entities in the database based on the value objects passed in
     * 
     * @param geneSetVos value objects constructed on the client.
     * @return value objects converted from the newly created entities
     */
    public Collection<GeneSetValueObject> create( Collection<GeneSetValueObject> geneSetVos ) {

        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();
        for ( GeneSetValueObject geneSetVo : geneSetVos ) {

            if ( geneSetVo.getGeneIds() == null || geneSetVo.getGeneIds().isEmpty() ) {
                throw new IllegalArgumentException( "No gene ids provided. Cannot save an empty set." );
            }

            results.add( create( geneSetVo ) );

        }
        return results;
    }

    /**
     * create an entity in the database based on the value object parameter
     * 
     * @param eesvo
     * @return value object converted from the newly created entity
     */
    private GeneSetValueObject create( GeneSetValueObject obj ) {

        if ( obj.getId() != null && obj.getId() >= 0 ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "Gene group name cannot be blank" );
        }

        return geneSetService.createDatabaseEntity( obj );
    }

    /**
     * AJAX adds the gene group to the session, used by SessionGeneGroupStore and SessionDatasetGroupStore sets the
     * groups taxon value and reference
     * 
     * @param geneSetVo value object constructed on the client.
     * @param modificationBased whether the set was modified by the user
     * @return collection of added session groups (with updated reference.id etc)
     */
    public Collection<SessionBoundGeneSetValueObject> addSessionGroups(
            Collection<SessionBoundGeneSetValueObject> geneSetVos, Boolean modificationBased ) {

        Collection<SessionBoundGeneSetValueObject> results = new HashSet<SessionBoundGeneSetValueObject>();

        for ( SessionBoundGeneSetValueObject gsvo : geneSetVos ) {
            TaxonValueObject tax = geneSetService.getGeneSetTaxon( gsvo );
            gsvo.setTaxonId( tax.getId() );
            gsvo.setTaxonName( tax.getCommonName() );

            // sets the reference and stores the group
            results.add( sessionListManager.addGeneSet( gsvo, modificationBased ) );

        }

        return results;
    }

    /**
     * AJAX adds the gene group to the session
     * 
     * @param geneSetVo value object constructed on the client.
     * @return id of the new gene group
     */
    public Collection<GeneSetValueObject> addUserAndSessionGroups( Collection<GeneSetValueObject> geneSetVos ) {

        Collection<GeneSetValueObject> result = new HashSet<GeneSetValueObject>();

        Collection<SessionBoundGeneSetValueObject> sessionResult = new HashSet<SessionBoundGeneSetValueObject>();

        for ( GeneSetValueObject gsvo : geneSetVos ) {

            if ( gsvo instanceof SessionBoundGeneSetValueObject ) {
                sessionResult.add( ( SessionBoundGeneSetValueObject ) gsvo );
            } else {
                result.add( gsvo );
            }

        }

        result = create( result );

        result.addAll( addSessionGroups( sessionResult, true ) );

        return result;
    }

    /**
     * Given a Gemma Gene Id will find all gene groups that the current user is allowed to use
     * 
     * @param geneId
     * @return collection of geneSetValueObject
     */
    public Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId ) {

        return findGeneSetsByGene( geneId );
    }

    /**
     * @param query string to match to a gene set.
     * @param taxonId
     * @return collection of GeneSetValueObject
     */
    public Collection<GeneSetValueObject> findGeneSetsByName( String query, Long taxonId ) {

        return geneSetService.findGeneSetsByName( query, taxonId );
    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the group and whether the group is db-backed
     * 
     * @param s
     * @return
     */
    public String canCurrentUserEditGroup( GeneSetValueObject gsvo ) {
        boolean userCanEditGroup = false;
        boolean groupIsDBBacked = false;
        if ( gsvo instanceof DatabaseBackedGeneSetValueObject ) {
            groupIsDBBacked = true;
            try {
                userCanEditGroup = securityService.isEditable( geneSetService.load( gsvo.getId() ) );
            } catch ( org.springframework.security.access.AccessDeniedException ade ) {
                return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + false + "}";
            }
        }

        return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + userCanEditGroup + "}";
    }

    /**
     * AJAX If the current user has access to given gene group, will return the gene value objects in the gene group
     * 
     * @param groupId
     * @return
     */
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        return geneSetService.getGenesInGroup( groupId );

    }

    /**
     * AJAX Returns just the current users gene sets
     * 
     * @param privateOnly
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return
     */
    public Collection<DatabaseBackedGeneSetValueObject> getUsersGeneGroups( boolean privateOnly, Long taxonId ) {

        return geneSetService.getUsersGeneGroups( privateOnly, taxonId );
    }

    /**
     * AJAX Returns the current users gene sets as well as their session gene sets
     * 
     * @param privateOnly
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return
     */
    public Collection<GeneSetValueObject> getUserAndSessionGeneGroups( boolean privateOnly, Long taxonId ) {

        Collection<GeneSetValueObject> result = new ArrayList<GeneSetValueObject>();

        Collection<DatabaseBackedGeneSetValueObject> dbresult = getUsersGeneGroups( privateOnly, taxonId );

        // TODO implement taxonId filtering when taxon gets added to GeneSetValueObject
        Collection<SessionBoundGeneSetValueObject> sessionResult = sessionListManager.getAllGeneSets( taxonId );

        result.addAll( dbresult );
        result.addAll( sessionResult );

        return result;
    }

    /**
     * AJAX Returns just the current users gene sets
     * 
     * @param privateOnly
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return
     */
    public Collection<SessionBoundGeneSetValueObject> getUserSessionGeneGroups( boolean privateOnly, Long taxonId ) {

        // TODO implement taxonId filtering when taxon gets added to GeneSetValueObject
        Collection<SessionBoundGeneSetValueObject> result = sessionListManager.getAllGeneSets( taxonId );
        return result;
    }

    /**
     * AJAX Given a valid gene group will remove it from db (if the user has permissons to do so). TODO do we need to
     * return an empty set?
     * 
     * @param groups
     */
    public Collection<DatabaseBackedGeneSetValueObject> remove( Collection<DatabaseBackedGeneSetValueObject> vos ) {
        geneSetService.deleteDatabaseEntities( vos );
        return new HashSet<DatabaseBackedGeneSetValueObject>();
    }

    /**
     * AJAX Given a valid gene group will remove it from the session.
     * 
     * @param groups
     */
    public Collection<SessionBoundGeneSetValueObject> removeSessionGroups(
            Collection<SessionBoundGeneSetValueObject> vos ) {
        for ( SessionBoundGeneSetValueObject geneSetValueObject : vos ) {
            sessionListManager.removeGeneSet( geneSetValueObject );
        }

        return new HashSet<SessionBoundGeneSetValueObject>();
    }

    /**
     * AJAX Given valid gene groups will remove them from the session or the database appropriately.
     * 
     * @param groups
     */
    public Collection<GeneSetValueObject> removeUserAndSessionGroups( Collection<GeneSetValueObject> vos ) {

        Collection<GeneSetValueObject> removedSets = new HashSet<GeneSetValueObject>();
        Collection<DatabaseBackedGeneSetValueObject> databaseCollection = new HashSet<DatabaseBackedGeneSetValueObject>();
        Collection<SessionBoundGeneSetValueObject> sessionCollection = new HashSet<SessionBoundGeneSetValueObject>();

        for ( GeneSetValueObject geneSetValueObject : vos ) {
            if ( geneSetValueObject instanceof SessionBoundGeneSetValueObject ) {
                sessionCollection.add( ( SessionBoundGeneSetValueObject ) geneSetValueObject );
            } else if ( geneSetValueObject instanceof DatabaseBackedGeneSetValueObject ) {
                databaseCollection.add( ( DatabaseBackedGeneSetValueObject ) geneSetValueObject );
            }
        }

        sessionCollection = removeSessionGroups( sessionCollection );
        databaseCollection = remove( databaseCollection );

        removedSets.addAll( sessionCollection );
        removedSets.addAll( databaseCollection );

        return removedSets;
    }

    /**
     * AJAX Updates the session group.
     * 
     * @param groups
     */
    public Collection<SessionBoundGeneSetValueObject> updateSessionGroups(
            Collection<SessionBoundGeneSetValueObject> vos ) {
        for ( SessionBoundGeneSetValueObject geneSetValueObject : vos ) {
            sessionListManager.updateGeneSet( geneSetValueObject );
        }
        return vos;
    }

    /**
     * AJAX Updates the session group and user database groups.
     * 
     * @param groups
     */
    public Collection<GeneSetValueObject> updateUserAndSessionGroups( Collection<GeneSetValueObject> vos ) {

        Collection<GeneSetValueObject> updatedSets = new HashSet<GeneSetValueObject>();
        Collection<DatabaseBackedGeneSetValueObject> databaseCollection = new HashSet<DatabaseBackedGeneSetValueObject>();
        Collection<SessionBoundGeneSetValueObject> sessionCollection = new HashSet<SessionBoundGeneSetValueObject>();

        for ( GeneSetValueObject geneSetValueObject : vos ) {
            if ( geneSetValueObject instanceof SessionBoundGeneSetValueObject ) {
                sessionCollection.add( ( SessionBoundGeneSetValueObject ) geneSetValueObject );
            } else if ( geneSetValueObject instanceof DatabaseBackedGeneSetValueObject ) {
                databaseCollection.add( ( DatabaseBackedGeneSetValueObject ) geneSetValueObject );
            }
        }

        sessionCollection = updateSessionGroups( sessionCollection );
        databaseCollection = update( databaseCollection );

        updatedSets.addAll( sessionCollection );
        updatedSets.addAll( databaseCollection );

        return updatedSets;

    }

    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice.
     * 
     * @param groupId
     * @param geneIds
     */
    public DatabaseBackedGeneSetValueObject updateNameDesc( DatabaseBackedGeneSetValueObject geneSetVO ) {

        return geneSetService.updateDatabaseEntityNameDesc( geneSetVO );

    }

    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice.
     * 
     * @param groupId
     * @param geneIds
     */
    public Collection<DatabaseBackedGeneSetValueObject> update( Collection<DatabaseBackedGeneSetValueObject> geneSetVos ) {

        return geneSetService.updateDatabaseEntity( geneSetVos );

    }

    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice. Cannot update name or description, just members
     * 
     * @param groupId id of the gene set being updated
     * @param geneIds
     */
    public String updateMembers( Long groupId, Collection<Long> geneIds ) {

        return geneSetService.updateDatabaseEntityMembers( groupId, geneIds );

    }

    /**
     * AJAX If the current user has access to given gene group will return the gene ids in the gene group
     * 
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/showGeneSet.html", method = RequestMethod.GET)
    public ModelAndView showGeneSet( HttpServletRequest request, HttpServletResponse response ) {

        ModelAndView mav = new ModelAndView( "geneSet.detail" );

        // if this is slow, we can get rid of it
        // checking the id here rather than in the js widget gives better error feedback
        // though it doesn mean we load the set twice
        GeneSetValueObject geneSet = getGeneSetFromRequest( request );
        mav.addObject( "geneSetId", geneSet.getId() );
        mav.addObject( "geneSetName", geneSet.getName() );

        return mav;
    }

    /**
     * @param request
     * @return
     * @throws IllegalArgumentException if a matching EE can't be loaded
     */
    private GeneSetValueObject getGeneSetFromRequest( HttpServletRequest request ) {

        GeneSetValueObject geneSet = null;
        Long id = null;

        if ( request.getParameter( "id" ) != null ) {
            try {
                id = Long.parseLong( request.getParameter( "id" ) );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "You must provide a valid numerical identifier" );
            }
            geneSet = geneSetService.getValueObject( id );

            if ( geneSet == null ) {
                throw new IllegalArgumentException( "Unable to access gene set with id=" + id );
            }
        } else {
            throw new IllegalArgumentException( "You must provide an id" );
        }
        return geneSet;
    }

    /**
     * AJAX
     * 
     * @param groupId
     * @return
     */
    public DatabaseBackedGeneSetValueObject load( Long id ) {

        if ( id == null ) {
            throw new IllegalArgumentException( "Cannot load a gene set with a null id." );
        }
        return geneSetService.getValueObject( id );

    }

}
