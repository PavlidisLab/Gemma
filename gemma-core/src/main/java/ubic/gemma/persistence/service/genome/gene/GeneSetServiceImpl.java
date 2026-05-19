/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import ubic.gemma.core.security.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import org.springframework.lang.Nullable;
import java.util.*;

/**
 * Service for managing gene sets
 *
 * @author kelsey
 */
@Service
@Slf4j
public class GeneSetServiceImpl extends AbstractVoEnabledService<GeneSet, DatabaseBackedGeneSetValueObject> implements GeneSetService {

    private static final Double DEFAULT_SCORE = 0.0;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneSetDao geneSetDao = null;

    @Autowired
    private GeneSetReadService readService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    public GeneSetServiceImpl( GeneSetDao voDao ) {
        super( voDao );
    }

    // =====================================================================
    // Read methods -- delegate to GeneSetReadService.
    // ACL @Secured / @PostFilter annotations live on the GeneSetService
    // interface and apply at the facade proxy boundary; callers that need
    // permission filtering MUST go through this facade rather than injecting
    // GeneSetReadService directly.
    // =====================================================================

    @Override
    public Collection<GeneSet> loadWithMembers( Collection<Long> ids ) {
        return readService.loadWithMembers( ids );
    }

    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        return readService.findByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseBackedGeneSetValueObject loadValueObject( GeneSet geneSet ) {
        return geneSetDao.loadValueObject( geneSet );
    }

    @Override
    public DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id ) {
        return readService.loadValueObjectByIdLite( id );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        return this.geneSetDao.loadValueObjectsByIds( ids );

    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> genesetIds ) {
        return readService.loadValueObjectsByIdsLite( genesetIds );
    }

    @Override
    public Collection<GeneSet> findByName( String name ) {
        return readService.findByName( name );
    }

    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return readService.findByName( name, taxon );
    }

    @Override
    public Collection<GeneSet> loadAll( @Nullable Taxon tax ) {
        return readService.loadAll( tax );
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets() {
        return readService.loadMyGeneSets();
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return readService.loadMyGeneSets( tax );
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets( Taxon tax ) {
        return readService.loadMySharedGeneSets( tax );
    }

//    @Override
//    @Transactional
//    public void addGene(GeneSet geneset, Gene gene) {
//        this.geneSetDao.addGeneToSet(geneset, gene);
//    }
//    
//    @Override
//    @Transactional
//    public void removeGene(GeneSet geneset, Gene gene) {
//        this.geneSetDao.removeGeneFromSet(geneset, gene);
//    }

    @Override
    @Transactional
    public GeneSetValueObject createDatabaseEntity( GeneSetValueObject geneSetVo ) {
        GeneSet newGeneSet = GeneSet.Factory.newInstance();
        newGeneSet.setName( geneSetVo.getName() );
        newGeneSet.setDescription( geneSetVo.getDescription() );

        Collection<Long> geneIds = geneSetVo.getGeneIds();

        // If no gene Ids just create group and return.
        if ( geneIds != null && !geneIds.isEmpty() ) {
            Collection<Gene> genes = geneService.load( geneIds );

            if ( geneIds.size() != genes.size() ) {
                log.warn( "Not all genes were found by id: " + geneIds.size() + " ids, " + genes.size()
                        + " genes fetched" );
            }

            Set<GeneSetMember> geneMembers = new HashSet<>();
            for ( Gene g : genes ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( g );
                gmember.setScore( GeneSetServiceImpl.DEFAULT_SCORE );
                geneMembers.add( gmember );
            }

            newGeneSet.setMembers( geneMembers );
        }

        GeneSet gset = this.create( newGeneSet );

        // make groups private by default
        // can't do this to newGeneSet variable because the entity's id needs to be non-null
        if ( geneSetVo.getIsPublic() ) {
            securityService.makePublic( gset );
        } else {
            securityService.makePrivate( gset );
        }

        return geneSetValueObjectHelper.convertToValueObject( this.load( gset.getId() ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId ) {

        Gene gene = geneService.loadOrFail( geneId );

        Collection<GeneSet> genesets = geneSetSearch.findByGene( gene );

        Collection<GeneSetValueObject> gsvos = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor // not possible safely
        gsvos.addAll( geneSetValueObjectHelper.convertToValueObjects( genesets, false ) );
        return gsvos;
    }

    @Override
    @Transactional
    public DatabaseBackedGeneSetValueObject updateDatabaseEntityNameDesc( DatabaseBackedGeneSetValueObject geneSetVO ) {

        Long groupId = geneSetVO.getId();
        GeneSet gset = this.load( groupId );
        if ( gset == null ) {
            throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
        }

        gset.setDescription( geneSetVO.getDescription() );
        if ( geneSetVO.getName() != null && geneSetVO.getName().length() > 0 )
            gset.setName( geneSetVO.getName() );
        this.update( gset );

        return geneSetValueObjectHelper.convertToValueObject( gset );

    }

    @Override
    @Transactional
    public void updateDatabaseEntityMembers( Long groupId, Collection<Long> geneIds ) {

        GeneSet gSet = this.load( groupId );
        if ( gSet == null ) {
            throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
        }
        Collection<GeneSetMember> updatedGenelist = new HashSet<>();

        if ( geneIds.isEmpty() ) {
            throw new IllegalArgumentException( "No gene ids provided. Cannot save an empty set." );
        }

        Collection<Gene> genes = geneService.load( geneIds );

        if ( genes.isEmpty() ) {
            throw new IllegalArgumentException(
                    "None of the gene ids were valid (out of " + geneIds.size() + " provided)" );
        }
        if ( genes.size() < geneIds.size() ) {
            throw new IllegalArgumentException(
                    "Some of the gene ids were invalid: only found " + genes.size() + " out of " + geneIds.size()
                            + " provided)" );
        }

        assert genes.size() == geneIds.size();

        this.checkGeneList( gSet, updatedGenelist, genes );

        gSet.getMembers().clear();
        gSet.getMembers().addAll( updatedGenelist );

        this.update( gSet );
    }

    @Override
    @Transactional
    public Collection<DatabaseBackedGeneSetValueObject> updateDatabaseEntity(
            Collection<DatabaseBackedGeneSetValueObject> geneSetVos ) {

        Collection<GeneSet> updated = new HashSet<>();
        for ( DatabaseBackedGeneSetValueObject geneSetVo : geneSetVos ) {

            Long groupId = geneSetVo.getId();
            GeneSet gset = this.load( groupId );
            if ( gset == null ) {
                throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
            }
            Collection<GeneSetMember> updatedGenelist = new HashSet<>();

            Collection<Long> geneIds = geneSetVo.getGeneIds();

            if ( geneIds == null || geneIds.isEmpty() ) {
                throw new IllegalArgumentException( "No gene ids provided. Cannot save an empty set." );
            }

            Collection<Gene> genes = geneService.load( geneIds );

            if ( genes.isEmpty() ) {
                throw new IllegalArgumentException(
                        "None of the gene ids were valid (out of " + geneIds.size() + " provided)" );
            }
            if ( genes.size() < geneIds.size() ) {
                throw new IllegalArgumentException(
                        "Some of the gene ids were invalid: only found " + genes.size() + " out of " + geneIds.size()
                                + " provided)" );
            }

            assert genes.size() == geneIds.size();

            this.checkGeneList( gset, updatedGenelist, genes );

            gset.getMembers().clear();
            gset.getMembers().addAll( updatedGenelist );
            gset.setDescription( geneSetVo.getDescription() );
            gset.setName( geneSetVo.getName() );
            this.update( gset );

            /*
             * Make sure we return the latest.
             */
            updated.add( this.load( gset.getId() ) );
        }
        return geneSetValueObjectHelper.convertToValueObjects( updated );

    }

    @Override
    @Transactional
    public void deleteDatabaseEntity( DatabaseBackedGeneSetValueObject geneSetVO ) {
        GeneSet gset = this.load( geneSetVO.getId() );
        if ( gset != null )
            this.remove( gset );
    }

    @Override
    @Transactional
    public void deleteDatabaseEntities( Collection<DatabaseBackedGeneSetValueObject> vos ) {
        for ( DatabaseBackedGeneSetValueObject geneSetValueObject : vos ) {
            this.deleteDatabaseEntity( geneSetValueObject );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> getUsersGeneGroups( boolean privateOnly, @Nullable Long taxonId, boolean sharedPublicOnly ) {

        Taxon tax = null;
        if ( taxonId != null ) {
            tax = taxonService.load( taxonId );
            if ( tax == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
        }

        Collection<GeneSet> geneSets;

        if ( privateOnly ) {
            // gets all groups user can see (includes: owned by user, shared with user & public)
            geneSets = this.loadAll( tax );

            // this filtering is to filter out public sets
            try {
                if ( !geneSets.isEmpty() ) {
                    geneSets.retainAll( securityService.choosePrivate( geneSets ) );
                }
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else if ( sharedPublicOnly ) {
            // gets all groups shared with the user and all groups owned by the user, except public ones
            geneSets = this.loadMySharedGeneSets( tax );
        } else {
            geneSets = this.loadAll( tax );
        }

        return geneSets;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DatabaseBackedGeneSetValueObject> getUsersGeneGroupsValueObjects( boolean privateOnly,
            Long taxonId ) {
        Collection<GeneSet> geneSets = this.getUsersGeneGroups( privateOnly, taxonId, false );
        return geneSetValueObjectHelper.convertToValueObjects( geneSets );
    }

    @Override
    public Collection<GeneValueObject> getGenesInGroup( GeneSetValueObject object ) {
        return readService.getGenesInGroup( object );
    }

    @Override
    public Collection<Long> getGeneIdsInGroup( GeneSetValueObject object ) {
        return readService.getGeneIdsInGroup( object );
    }

    @Override
    public int getSize( GeneSetValueObject object ) {
        return readService.getSize( object );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSetValueObject> findGeneSetsByName( String query, @Nullable Long taxonId ) throws SearchException {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<>();
        }
        Collection<GeneSet> foundGeneSets;
        Taxon tax = null;
        if ( taxonId == null ) {
            // throw new IllegalArgumentException( "Taxon must not be null" );
            foundGeneSets = geneSetSearch.findByName( query );
        } else {

            tax = taxonService.load( taxonId );

            if ( tax == null ) {
                // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
                foundGeneSets = geneSetSearch.findByName( query );
            } else {
                foundGeneSets = geneSetSearch.findByName( query, tax );
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
                if ( goSet != null )
                    foundGeneSets.add( goSet );
            } else {
                foundGeneSets.addAll( geneSetSearch.findByGoTermName( query, tax ) );
            }
        }

        Collection<GeneSetValueObject> gsvos = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor // Not possible safely
        gsvos.addAll( geneSetValueObjectHelper.convertToValueObjects( foundGeneSets ) );
        return gsvos;
    }

    @Override
    public TaxonValueObject getTaxonVOforGeneSetVO( SessionBoundGeneSetValueObject geneSetVO ) {
        return readService.getTaxonVOforGeneSetVO( geneSetVO );
    }

    @Override
    public Taxon getTaxon( GeneSet geneSet ) {
        return readService.getTaxon( geneSet );
    }

    @Override
    public Set<Taxon> getTaxa( GeneSet geneSet ) {
        return readService.getTaxa( geneSet );
    }

    private void checkGeneList( GeneSet gset, Collection<GeneSetMember> updatedGenelist, Collection<Gene> genes ) {
        for ( Gene g : genes ) {

            GeneSetMember gsm = GeneSet.containsGene( g, gset );

            // Gene not in list create memember and add it.
            if ( gsm == null ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( g );
                gmember.setScore( GeneSetServiceImpl.DEFAULT_SCORE );
                gset.getMembers().add( gmember );
                updatedGenelist.add( gmember );
            } else {
                updatedGenelist.add( gsm );
            }
        }
    }

    @Override
    @Transactional
    public int removeAll() {
        return geneSetDao.removeAll();
    }
}
