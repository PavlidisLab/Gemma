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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import edu.emory.mathcs.backport.java.util.Collections;
import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetImpl;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.web.persistence.SessionListManager;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.GeneSetSearch;
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

    private static final Double DEFAULT_SCORE = 0.0;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private SecurityService securityService = null; 

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private SessionListManager sessionListManager;

    /**
     * AJAX Creates a new gene group given a name for the group and the genes in the group
     * 
     * @param geneSetVo value object constructed on the client.
     * @return id of the new gene group
     */
    public Collection<GeneSetValueObject> create( Collection<GeneSetValueObject> geneSetVos ) {
        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();
        for ( GeneSetValueObject geneSetVo : geneSetVos ) {

            if ( StringUtils.isBlank( geneSetVo.getName() ) ) {
                throw new IllegalArgumentException( "Gene group name cannot be blank" );
            }
            if ( geneSetVo.getGeneIds() == null || geneSetVo.getGeneIds().isEmpty() ) {
                throw new IllegalArgumentException( "No gene ids provided. Cannot save an empty set." );
            }

            GeneSet gset = create( geneSetVo );
            results.add( new DatabaseBackedGeneSetValueObject( gset ) );

        }
        return results;
    }
    
    private Collection<SessionBoundGeneSetValueObject> setSessionGroupTaxonValues( Collection<SessionBoundGeneSetValueObject> geneSetVos ){

        
        for ( SessionBoundGeneSetValueObject gsvo : geneSetVos ) {
            
            // get taxon from members
            for (Long l : gsvo.getGeneIds()){
                Gene gene = geneService.load( l );
                
                if (gene!=null && gene.getTaxon()!=null){
                    gsvo.setTaxonId( gene.getTaxon().getId() );
                    gsvo.setTaxonName( gene.getTaxon().getCommonName() );
                    break;//assuming that the taxon will be the same for all genes in the set so no need to load all genes from set
                }
            }
        }
        
        return geneSetVos;
    }

    /**
     * AJAX adds the gene group to the session, used by SessionGeneGroupStore and SessionDatasetGroupStore
     * 
     * sets the groups taxon value and reference
     * 
     * @param geneSetVo value object constructed on the client.
     * @return collection of added session groups (with updated reference.id etc)
     */
    public Collection<SessionBoundGeneSetValueObject> addSessionGroups( Collection<SessionBoundGeneSetValueObject> geneSetVos ) {

        Collection<SessionBoundGeneSetValueObject> results = new HashSet<SessionBoundGeneSetValueObject>();
        
        geneSetVos = setSessionGroupTaxonValues( geneSetVos );

        for ( SessionBoundGeneSetValueObject gsvo : geneSetVos ) {
                        
            // sets the reference and stores the group
            results.add( sessionListManager.addGeneSet( gsvo ) );

        }

        return results;
    }

    /**
     * AJAX adds these gene groups to the session-bound list for groups that have been modified by the user
     * 
     * sets the groups taxon value and reference.id
     * 
     * @param geneSetVo value object constructed on the client.
     * @return collection of added session groups (with updated reference.id etc)
     */
    public Collection<SessionBoundGeneSetValueObject> addNonModificationBasedSessionBoundGroups( Collection<SessionBoundGeneSetValueObject> geneSetVos ) {

        Collection<SessionBoundGeneSetValueObject> results = new HashSet<SessionBoundGeneSetValueObject>();

        geneSetVos = setSessionGroupTaxonValues( geneSetVos );
        
        for ( SessionBoundGeneSetValueObject gsvo : geneSetVos ) {

            // sets the reference and stores the group
            results.add( sessionListManager.addGeneSet( gsvo , false) );

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
                sessionResult.add( (SessionBoundGeneSetValueObject) gsvo );
            } else {
                result.add( gsvo );
            }

        }

        result = create( result );

        result.addAll( addSessionGroups( sessionResult ) );

        return result;
    }

    /**
     * Given a Gemma Gene Id will find all gene groups that the current user is allowed to use
     * 
     * @param geneId
     * @return collection of geneSetValueObject
     */
    public Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId ) {

        Gene gene = geneService.load( geneId );

        Collection<GeneSet> genesets = this.geneSetSearch.findByGene( gene );

        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( DatabaseBackedGeneSetValueObject.convert2ValueObjects( genesets, false ) );
        return gsvos;
    }

    /**
     * @param query string to match to a gene set.
     * @param taxonId
     * @return collection of GeneSetValueObject
     */
    public Collection<GeneSetValueObject> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSetValueObject>();
        }
        Collection<GeneSet> foundGeneSets = null;
        Taxon tax = null;
        if ( taxonId == null ) {
            // throw new IllegalArgumentException( "Taxon must not be null" );
            foundGeneSets = this.geneSetSearch.findByName( query );
        } else {

            tax = taxonService.load( taxonId );

            if ( tax == null ) {
                // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
                foundGeneSets = this.geneSetSearch.findByName( query );
            } else {
                foundGeneSets = this.geneSetSearch.findByName( query, tax );
            }
        }

        /*
         * Behaviour implemented here (easy to change): If we have a match in our system we stop here. Otherwise, we go
         * on to search the Gene Ontology.
         */

        // need taxon ID to be set for now, easy to change in Gene2GOAssociationDaoImpl.handleFindByGoTerm(String,
        // Taxon)

        if ( foundGeneSets.isEmpty() && tax != null ) {
            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSet goSet = this.geneSetSearch.findByGoId( query, tax );
                if ( goSet != null ) foundGeneSets.add( goSet );
            } else {
                foundGeneSets.addAll( geneSetSearch.findByGoTermName( query, tax ) );
            }
        }

        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( DatabaseBackedGeneSetValueObject.convert2ValueObjects( foundGeneSets, false ) );
        return gsvos;
    }

    /**
     * AJAX 
     * returns a JSON string encoding whether the current user owns the group and whether the group is db-backed
     * @param s
     * @return
     */
    public String canCurrentUserEditGroup(GeneSetValueObject gsvo){
        boolean userCanEditGroup = false;
        boolean groupIsDBBacked = false;
        if(gsvo instanceof DatabaseBackedGeneSetValueObject){
            groupIsDBBacked = true;
            try{
                userCanEditGroup = securityService.isEditable( geneSetService.load( gsvo.getId() ) );
            }catch(org.springframework.security.access.AccessDeniedException ade){
                return "{groupIsDBBacked:"+groupIsDBBacked+",userCanEditGroup:"+false+"}";
            }
        }
        
        return "{groupIsDBBacked:"+groupIsDBBacked+",userCanEditGroup:"+userCanEditGroup+"}";
    }
    
    
    /**
     * AJAX If the current user has access to given gene group will return the gene ids in the gene group
     * 
     * @param groupId
     * @return
     */
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        Collection<GeneValueObject> results = null;

        GeneSet gs = geneSetService.load( groupId );
        if ( gs == null ) return null; // FIXME: Send and error code/feedback?

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    /**
     * AJAX Returns just the current users gene sets
     * 
     * @param privateOnly
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return
     */
    public Collection<DatabaseBackedGeneSetValueObject> getUsersGeneGroups( boolean privateOnly, Long taxonId ) {

        Taxon tax = null;
        if ( taxonId != null ) {
            tax = taxonService.load( taxonId );
            if ( tax == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
        }

        Collection<GeneSet> geneSets = new HashSet<GeneSet>();
        if ( privateOnly ) {
            try {
                geneSets = geneSetService.loadMyGeneSets( tax );
                geneSets.retainAll( securityService.choosePrivate( geneSets ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }

        } else {
            geneSets = geneSetService.loadAll( tax );
        }

        Collection<DatabaseBackedGeneSetValueObject> result = makeValueObects( geneSets );
        return result;
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
        Collection<SessionBoundGeneSetValueObject> sessionResult = sessionListManager.getAllGeneSets(taxonId);

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
        Collection<SessionBoundGeneSetValueObject> result = sessionListManager.getAllGeneSets(taxonId);
        return result;
    }

    /**
     * AJAX Given a valid gene group will remove it from db (if the user has permissons to do so).
     * 
     * @param groups
     */
    public Collection<DatabaseBackedGeneSetValueObject> remove( Collection<DatabaseBackedGeneSetValueObject> vos ) {
        for ( DatabaseBackedGeneSetValueObject geneSetValueObject : vos ) {
            GeneSet gset = geneSetService.load( geneSetValueObject.getId() );
            if ( gset != null ) geneSetService.remove( gset );
        }
        return new HashSet<DatabaseBackedGeneSetValueObject>();
    }

    /**
     * AJAX Given a valid gene group will remove it from the session.
     * 
     * @param groups
     */
    public Collection<SessionBoundGeneSetValueObject> removeSessionGroups( Collection<SessionBoundGeneSetValueObject> vos ) {
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
                sessionCollection.add( (SessionBoundGeneSetValueObject) geneSetValueObject );
            } else if (geneSetValueObject instanceof DatabaseBackedGeneSetValueObject) {
                databaseCollection.add( (DatabaseBackedGeneSetValueObject) geneSetValueObject );
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
    public Collection<SessionBoundGeneSetValueObject> updateSessionGroups( Collection<SessionBoundGeneSetValueObject> vos ) {
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
                sessionCollection.add( (SessionBoundGeneSetValueObject) geneSetValueObject );
            } else if (geneSetValueObject instanceof DatabaseBackedGeneSetValueObject) {
                databaseCollection.add( (DatabaseBackedGeneSetValueObject) geneSetValueObject );
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

        Long groupId = geneSetVO.getId();
        GeneSet gset = geneSetService.load( groupId );
        if ( gset == null ) {
            throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
        }

        gset.setDescription( geneSetVO.getDescription() );
        if ( geneSetVO.getName() != null && geneSetVO.getName().length() > 0 ) gset.setName( geneSetVO.getName() );
        geneSetService.update( gset );
        
        return new DatabaseBackedGeneSetValueObject( gset );

    }
    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice.
     * 
     * @param groupId
     * @param geneIds
     */
    public Collection<DatabaseBackedGeneSetValueObject> update( Collection<DatabaseBackedGeneSetValueObject> geneSetVos ) {

        Collection<GeneSet> updated = new HashSet<GeneSet>();
        for ( DatabaseBackedGeneSetValueObject geneSetVo : geneSetVos ) {

            Long groupId = geneSetVo.getId();
            GeneSet gset = geneSetService.load( groupId );
            if ( gset == null ) {
                throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
            }
            Collection<GeneSetMember> updatedGenelist = new HashSet<GeneSetMember>();

            Collection<Long> geneIds = geneSetVo.getGeneIds();

            if ( geneIds.isEmpty() ) {
                throw new IllegalArgumentException( "No gene ids provided. Cannot save an empty set." );

            }
            Collection<Gene> genes = geneService.loadMultiple( geneIds );

            if ( genes.isEmpty() ) {
                throw new IllegalArgumentException( "None of the gene ids were valid (out of " + geneIds.size()
                        + " provided)" );
            }
            if ( genes.size() < geneIds.size() ) {
                throw new IllegalArgumentException( "Some of the gene ids were invalid: only found " + genes.size()
                        + " out of " + geneIds.size() + " provided)" );
            }

            assert genes.size() == geneIds.size();

            for ( Gene g : genes ) {

                GeneSetMember gsm = GeneSetImpl.containsGene( g, gset );

                // Gene not in list create memember and add it.
                if ( gsm == null ) {
                    GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                    gmember.setGene( g );
                    gmember.setScore( DEFAULT_SCORE );
                    gset.getMembers().add( gmember );
                    updatedGenelist.add( gmember );
                } else {
                    updatedGenelist.add( gsm );
                }

            }

            gset.getMembers().clear();
            gset.getMembers().addAll( updatedGenelist );
            gset.setDescription( geneSetVo.getDescription() );
            gset.setName( geneSetVo.getName() );
            geneSetService.update( gset );

            /*
             * Make sure we return the latest.
             */
            updated.add( geneSetService.load( gset.getId() ) );
        }
        return makeValueObects( updated );

    }
    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds 
     * Will not allow the same gene to be added to the gene set twice.
     * Cannot update name or description, just members
     * @param groupId id of the gene set being updated
     * @param geneIds
     */
    public String updateMembers( Long groupId, Collection<Long> geneIds ) {

        String msg = null;

        GeneSet gset = geneSetService.load( groupId );
        if ( gset == null ) {
            throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
        }
        Collection<GeneSetMember> updatedGenelist = new HashSet<GeneSetMember>();

        if ( geneIds.isEmpty() ) {
            throw new IllegalArgumentException( "No gene ids provided. Cannot save an empty set." );
        }
        
        Collection<Gene> genes = geneService.loadMultiple( geneIds );

        if ( genes.isEmpty() ) {
            throw new IllegalArgumentException( "None of the gene ids were valid (out of " + geneIds.size()
                    + " provided)" );
        }
        if ( genes.size() < geneIds.size() ) {
            throw new IllegalArgumentException( "Some of the gene ids were invalid: only found " + genes.size()
                    + " out of " + geneIds.size() + " provided)" );
        }

        assert genes.size() == geneIds.size();

        for ( Gene g : genes ) {

            GeneSetMember gsm = GeneSetImpl.containsGene( g, gset );

            // Gene not in list create memember and add it.
            if ( gsm == null ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( g );
                gmember.setScore( DEFAULT_SCORE );
                gset.getMembers().add( gmember );
                updatedGenelist.add( gmember );
            } else {
                updatedGenelist.add( gsm );
            }
        }

        gset.getMembers().clear();
        gset.getMembers().addAll( updatedGenelist );

        geneSetService.update( gset );

        return msg;

    }

    /**
     * @param geneSetVo
     * @return
     */
    private GeneSet create( GeneSetValueObject geneSetVo ) {
        GeneSet newGeneSet = GeneSet.Factory.newInstance();
        newGeneSet.setName( geneSetVo.getName() );
        newGeneSet.setDescription( geneSetVo.getDescription() );

        // If no gene Ids just create group and return.
        GeneSet gset = geneSetService.create( newGeneSet );

        Collection<Long> geneIds = geneSetVo.getGeneIds();

        if ( geneIds != null && !geneIds.isEmpty() ) {
            Collection<Gene> genes = geneService.loadMultiple( geneIds );
            for ( Gene g : genes ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( g );
                gmember.setScore( DEFAULT_SCORE );
                gset.getMembers().add( gmember );
            }

            geneSetService.update( gset );
        }
        
        // make groups private by default
        if(geneSetVo.isPublik()){
            securityService.makePublic( gset );  
        }else{
           securityService.makePrivate( gset ); 
        }
        
        return geneSetService.load( gset.getId() );
    }

    /**
     * @param secs
     * @return
     */
    private DatabaseBackedGeneSetValueObject makeValueObect( GeneSet gs ) {
        DatabaseBackedGeneSetValueObject gsvo = new DatabaseBackedGeneSetValueObject( gs );

        gsvo.setCurrentUserHasWritePermission( securityService.isEditable( gs ) );
        gsvo.setCurrentUserIsOwner( securityService.isOwnedByCurrentUser( gs ) );
        gsvo.setPublik( securityService.isPublic( gs ) );
        gsvo.setShared( securityService.isShared( gs ) );

        return gsvo;
    }
    
    /**
     * @param secs
     * @return
     */
    private Collection<DatabaseBackedGeneSetValueObject> makeValueObects( Collection<GeneSet> secs ) {
        // Create valueobject (need to add security info or would move this out into the valueobject...
        List<DatabaseBackedGeneSetValueObject> result = new ArrayList<DatabaseBackedGeneSetValueObject>();
        for ( GeneSet gs : secs ) {

            DatabaseBackedGeneSetValueObject gsvo = makeValueObect( gs );


            result.add( gsvo );
        }

        Collections.sort( result, new Comparator<GeneSetValueObject>() {
            @Override
            public int compare( GeneSetValueObject o1, GeneSetValueObject o2 ) {
                return -o1.getSize().compareTo( o2.getSize() );
            }
        } );

        return result;
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
        GeneSet geneSet= getGeneSetFromRequest( request );
        mav.addObject( "geneSetId", geneSet.getId() );
        mav.addObject( "geneSetName", geneSet.getName() );

        return mav;
    }
    

    /**
     * @param request
     * @return
     * @throws IllegalArgumentException if a matching EE can't be loaded
     */
    private GeneSet getGeneSetFromRequest( HttpServletRequest request ) {

        GeneSet geneSet = null;
        Long id = null;

        if ( request.getParameter( "id" ) != null ) {
            try {
                id = Long.parseLong( request.getParameter( "id" ) );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "You must provide a valid numerical identifier" );
            }
            geneSet = geneSetService.load( id );

            if ( geneSet == null ) {
                throw new IllegalArgumentException( "Unable to access gene set with id=" + id );
            }
        }else{
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
        
        if(id == null){
            throw new IllegalArgumentException( "Cannot load a gene set with a null id." );
        }
        GeneSet set = geneSetService.load( id );// filtered
        // by
        // security.
        if(set == null){
            throw new AccessDeniedException("No gene set exists with id="+id+" or you do not have permission to access it.");
        }
        return makeValueObect( set );

    }


}
