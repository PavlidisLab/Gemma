/*
 * The Gemma project Copyright (c) 2010 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetImpl;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.security.SecurityService;
import ubic.gemma.web.controller.common.auditAndSecurity.GeneSetValueObject;
import ubic.gemma.web.controller.common.auditAndSecurity.SidValueObject;

/**
 * Exposes GeneServices methods over ajax
 * 
 * @author kelsey
 * @version $Id
 */
@Controller
public class GeneSetController {

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private SecurityService securityService = null;

    private static Log log = LogFactory.getLog( GeneSetController.class );
    private static final Double DEFAULT_SCORE = 0.0;

    public GeneSetController() {
        super();
    }

    /**
     * AJAX Creates a new gene group given a name for the group and the genes in the group
     * 
     * @param name
     * @param genes
     * @return
     */
    public Long createGeneGroup( String name, Collection<Long> geneIds ) {

        if ( name == null || name.isEmpty() ) return null;

        GeneSet gset = GeneSet.Factory.newInstance();
        gset.setName( name );

        // If no gene Ids just create group and return.
        if ( geneIds == null || geneIds.isEmpty() ) {
            gset = geneSetService.create( gset );
            this.securityService.makePrivate( gset );
            return gset.getId();
        }

        Collection<Gene> genes = geneService.loadMultiple( geneIds );

        if ( genes == null || genes.isEmpty() ) return null;

        for ( Gene g : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( g );
            gmember.setScore( DEFAULT_SCORE );
            gset.getMembers().add( gmember );
        }

        gset = geneSetService.create( gset );
        this.securityService.makePrivate( gset );

        return gset.getId();
    }

    /**
     * AJAX Given a valid gene group will remove it from db (if the user has permissons to do so).
     * 
     * @param groupId
     */
    public void deleteGeneGroup( Long groupId ) {

        if ( groupId == null ) return;

        GeneSet gset = geneSetService.load( groupId );
        if ( gset != null ) geneSetService.remove( gset );

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
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice.
     * 
     * @param groupId
     * @param geneIds
     */
    public void updateGeneGroup( Long groupId, String description,  Collection<Long> geneIds ) {

        GeneSet gset = geneSetService.load( groupId );
        if(gset == null){
            log.warn("Atempt to update a group that doesn't exist. ID =  " + groupId);
            return;
        }
        Collection<GeneSetMember> updatedGenelist = new HashSet<GeneSetMember>(); // Creating a new gene list indirectly
        // allows for
        // easy deletion of gene group members

        // Create the empty gene group
        if ( geneIds == null || geneIds.isEmpty() ) {
            gset.setMembers( updatedGenelist );
            geneSetService.update( gset );
            return;
        }

        Collection<Gene> genes = geneService.loadMultiple( geneIds );

        if ( genes == null || genes.isEmpty() ) {
            log.warn( "GeneIds returned were valid GeneIds:  Terminating updated and changing nothing." );
            return;
        }

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

        gset.setMembers( updatedGenelist );
        gset.setDescription( description );
        geneSetService.update( gset );

        return;

    }

    /**
     * AJAX Returns just the current users gene sets
     * 
     * @param privateOnly
     * @return
     */
    public Collection<GeneSetValueObject> getUsersGeneGroups( boolean privateOnly ) {
        Collection<Securable> secs = new HashSet<Securable>();

        Collection<GeneSet> geneSets = null;
        if ( privateOnly ) {
            try {
                geneSets = geneSetService.loadMyGeneSets();
                secs.addAll( securityService.choosePrivate( geneSets ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            // secs.addAll( geneSets );
            secs.addAll( geneSetService.loadAll() );
        }

        // Create valueobject (need to add security info or would move this out into the valueobject...
        Collection<GeneSetValueObject> result = new HashSet<GeneSetValueObject>();
        for ( Securable gs : secs ) {

            GeneSetValueObject gsvo = new GeneSetValueObject( ( GeneSet ) gs );
            gsvo.setGeneMembers( null ); // For fear of to much data being passed back, client makes seperate call to
            // get gene group memembers
            gsvo.setPublik( securityService.isPublic( gs ) );
            gsvo.setShared( securityService.isShared( gs ) );
            gsvo.setOwner( new SidValueObject( securityService.getOwner( gs ) ) );

            result.add( gsvo );
        }
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

        Collection<GeneSet> genesets = this.geneSetService.findByGene( gene );

        return GeneSetValueObject.convert2ValueObjects( genesets );
    }

}
