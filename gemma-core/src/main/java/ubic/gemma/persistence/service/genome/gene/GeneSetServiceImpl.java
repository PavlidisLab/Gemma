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

import gemma.gsec.SecurityService;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * Service for managing gene sets
 *
 * @author kelsey
 */
@Service
@CommonsLog
@ParametersAreNonnullByDefault
public class GeneSetServiceImpl extends AbstractVoEnabledService<GeneSet, DatabaseBackedGeneSetValueObject> implements GeneSetService {

    private static final Double DEFAULT_SCORE = 0.0;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneSetDao geneSetDao = null;

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

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> loadWithMembers( Collection<Long> ids ) {
        Collection<GeneSet> geneSets = geneSetDao.load( ids );
        geneSets.forEach( gs -> {
            Hibernate.initialize( gs.getMembers() );
            gs.getMembers().forEach( member -> Hibernate.initialize( member.getGene() ) );
        } );
        return geneSets;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> findByGene( Gene gene ) {
        return this.geneSetDao.findByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseBackedGeneSetValueObject loadValueObject( GeneSet geneSet ) {
        return geneSetDao.loadValueObject( geneSet );
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id ) {
        return geneSetDao.loadValueObjectByIdLite( id );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        return this.geneSetDao.loadValueObjectsByIds( ids );

    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> genesetIds ) {
        return this.geneSetDao.loadValueObjectsByIdsLite( genesetIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> findByName( String name ) {
        return this.geneSetDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return this.geneSetDao.findByName( name, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> loadAll( @Nullable Taxon tax ) {
        return this.geneSetDao.loadAll( tax );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> loadMyGeneSets() {
        return this.geneSetDao.loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return this.geneSetDao.loadAll( tax );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneSet> loadMySharedGeneSets( Taxon tax ) {
        return this.geneSetDao.loadAll( tax );
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
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> getGenesInGroup( GeneSetValueObject object ) {

        Collection<GeneValueObject> results;

        GeneSet gs = this.load( object.getId() );
        if ( gs == null )
            return null;

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getGeneIdsInGroup( GeneSetValueObject object ) {
        DatabaseBackedGeneSetValueObject vo = loadValueObjectById( object.getId() );
        if ( vo == null ) {
            log.warn( String.format( "GeneSet %d was null when reloading it from the database, was it removed?", object.getId() ) );
            return Collections.emptySet();
        }
        return vo.getGeneIds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getSize( GeneSetValueObject object ) {
        return this.geneSetDao.getGeneCount( object.getId() );
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
    @Transactional(readOnly = true)
    public TaxonValueObject getTaxonVOforGeneSetVO( SessionBoundGeneSetValueObject geneSetVO ) {

        if ( geneSetVO == null )
            return null;

        if ( geneSetVO.getGeneIds() == null )
            return null;

        TaxonValueObject taxonVO = null;
        // get taxon from members
        for ( Long l : geneSetVO.getGeneIds() ) {
            Gene gene = geneService.load( l );

            if ( gene != null && gene.getTaxon() != null ) {
                taxonVO = TaxonValueObject.fromEntity( gene.getTaxon() );
                break;// assuming that the taxon will be the same for all genes in the set so no need to load all genes
                // from set
            }
        }

        return taxonVO;
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( GeneSet geneSet ) {
        return geneSetDao.getTaxon( geneSet );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Taxon> getTaxa( GeneSet geneSet ) {
        return new HashSet<>( geneSetDao.getTaxa( geneSet ) );
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
