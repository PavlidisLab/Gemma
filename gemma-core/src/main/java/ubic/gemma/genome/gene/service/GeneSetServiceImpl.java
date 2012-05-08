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
package ubic.gemma.genome.gene.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetDao;
import ubic.gemma.model.genome.gene.GeneSetImpl;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.security.SecurityService;

/**
 * Service for managing gene sets
 * 
 * @author kelsey
 * @version $Id$
 */
@Service
public class GeneSetServiceImpl implements GeneSetService {

    private static final Double DEFAULT_SCORE = 0.0;

    @Autowired
    private GeneSetDao geneSetDao = null;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#create(java.util.Collection)
     */
    @Override
    public Collection<GeneSet> create( Collection<GeneSet> sets ) {
        return ( Collection<GeneSet> ) this.geneSetDao.create( sets );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#create(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    public GeneSet create( GeneSet geneset ) {
        return this.geneSetDao.create( geneset );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        return this.geneSetDao.findByGene( gene );
    }

    /**
     * @param gene
     * @return
     */
    @Override
    public Collection<GeneSet> findByName( String name ) {
        return this.geneSetDao.findByName( name );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#findByName(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return this.geneSetDao.findByName( name, taxon );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#load(java.util.Collection)
     */
    @Override
    public Collection<GeneSet> load( Collection<Long> ids ) {
        return ( Collection<GeneSet> ) this.geneSetDao.load( ids );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#load(java.lang.Long)
     */
    @Override
    public GeneSet load( Long id ) {
        return this.geneSetDao.load( id );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#loadAll()
     */
    @Override
    public Collection<GeneSet> loadAll() {
        return ( Collection<GeneSet> ) this.geneSetDao.loadAll();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#loadAll(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneSet> loadAll( Taxon tax ) {
        return this.geneSetDao.loadAll( tax );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#loadMyGeneSets()
     */
    @Override
    public Collection<GeneSet> loadMyGeneSets() {
        return ( Collection<GeneSet> ) this.geneSetDao.loadMyGeneSets();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#loadMyGeneSets(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return ( Collection<GeneSet> ) this.geneSetDao.loadMyGeneSets( tax );
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets() {
        return ( Collection<GeneSet> ) this.geneSetDao.loadMySharedGeneSets();
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets( Taxon tax ) {
        return ( Collection<GeneSet> ) this.geneSetDao.loadMySharedGeneSets( tax );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#remove(java.util.Collection)
     */
    @Override
    public void remove( Collection<GeneSet> sets ) {
        this.geneSetDao.remove( sets );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#remove(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    public void remove( GeneSet geneset ) {
        this.geneSetDao.remove( geneset );
    }

    public void setGeneSetDao( GeneSetDao geneSetDao ) {
        this.geneSetDao = geneSetDao;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#update(java.util.Collection)
     */
    @Override
    public void update( Collection<GeneSet> sets ) {
        this.geneSetDao.update( sets );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneSetService#update(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    public void update( GeneSet geneset ) {
        this.geneSetDao.update( geneset );

    }

    @Override
    public DatabaseBackedGeneSetValueObject getValueObject( Long id ) {
        GeneSet geneSet = load( id );
        return geneSetValueObjectHelper.convertToValueObject( geneSet );
    }

    @Override
    public Collection<DatabaseBackedGeneSetValueObject> getValueObjects( Collection<Long> ids ) {
        Collection<DatabaseBackedGeneSetValueObject> vos = new ArrayList<DatabaseBackedGeneSetValueObject>();
        for ( Long id : ids ) {
            vos.add( getValueObject( id ) );
        }
        return vos;
    }

    /**
     * @param geneSetVo
     * @return
     */
    @Override
    public GeneSetValueObject createDatabaseEntity( GeneSetValueObject geneSetVo ) {
        GeneSet newGeneSet = GeneSet.Factory.newInstance();
        newGeneSet.setName( geneSetVo.getName() );
        newGeneSet.setDescription( geneSetVo.getDescription() );

        Collection<Long> geneIds = geneSetVo.getGeneIds();

        // If no gene Ids just create group and return.
        if ( geneIds != null && !geneIds.isEmpty() ) {
            Collection<Gene> genes = geneService.loadMultiple( geneIds );
            Collection<GeneSetMember> geneMembers = new HashSet<GeneSetMember>();
            for ( Gene g : genes ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( g );
                gmember.setScore( DEFAULT_SCORE );
                geneMembers.add( gmember );
            }

            newGeneSet.setMembers( geneMembers );
        }

        GeneSet gset = create( newGeneSet );

        // make groups private by default
        // can't do this to newGeneSet variable because the entity's id needs to be non-null
        if ( geneSetVo.isPublik() ) {
            securityService.makePublic( gset );
        } else {
            securityService.makePrivate( gset );
        }

        return geneSetValueObjectHelper.convertToValueObject( load( gset.getId() ) );
    }

    /**
     * Given a Gemma Gene Id will find all gene groups that the current user is allowed to use
     * 
     * @param geneId
     * @return collection of geneSetValueObject
     */
    @Override
    public Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId ) {

        Gene gene = geneService.load( geneId );

        Collection<GeneSet> genesets = geneSetSearch.findByGene( gene );

        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( geneSetValueObjectHelper.convertToValueObjects( genesets, false ) );
        return gsvos;
    }

    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice. Cannot update name or description, just members
     * 
     * @param groupId id of the gene set being updated
     * @param geneIds
     */
    @Override
    public String updateDatabaseEntityMembers( Long groupId, Collection<Long> geneIds ) {

        String msg = null;

        GeneSet gset = load( groupId );
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

        update( gset );

        return msg;

    }

    /**
     * AJAX Updates the database entity (permission permitting) with the fields of the param value object
     * 
     * @param groupId
     * @param geneIds
     * @return value objects for the updated entities
     */
    @Override
    public Collection<DatabaseBackedGeneSetValueObject> updateDatabaseEntity(
            Collection<DatabaseBackedGeneSetValueObject> geneSetVos ) {

        Collection<GeneSet> updated = new HashSet<GeneSet>();
        for ( DatabaseBackedGeneSetValueObject geneSetVo : geneSetVos ) {

            Long groupId = geneSetVo.getId();
            GeneSet gset = load( groupId );
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
            update( gset );

            /*
             * Make sure we return the latest.
             */
            updated.add( load( gset.getId() ) );
        }
        return geneSetValueObjectHelper.convertToValueObjects( updated );

    }

    /**
     * AJAX Updates the database entity (permission permitting) with the name and description fields of the param value
     * object
     * 
     * @param valueObject of database entity to update
     * @return value objects for the updated entities
     */
    @Override
    public DatabaseBackedGeneSetValueObject updateDatabaseEntityNameDesc( DatabaseBackedGeneSetValueObject geneSetVO ) {

        Long groupId = geneSetVO.getId();
        GeneSet gset = load( groupId );
        if ( gset == null ) {
            throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
        }

        gset.setDescription( geneSetVO.getDescription() );
        if ( geneSetVO.getName() != null && geneSetVO.getName().length() > 0 ) gset.setName( geneSetVO.getName() );
        update( gset );

        return geneSetValueObjectHelper.convertToValueObject( gset );

    }

    @Override
    public void deleteDatabaseEntity( DatabaseBackedGeneSetValueObject geneSetVO ) {
        GeneSet gset = load( geneSetVO.getId() );
        if ( gset != null ) remove( gset );
    }

    @Override
    public void deleteDatabaseEntities( Collection<DatabaseBackedGeneSetValueObject> vos ) {
        for ( DatabaseBackedGeneSetValueObject geneSetValueObject : vos ) {
            deleteDatabaseEntity( geneSetValueObject );
        }
    }

    /**
     * Returns all the gene sets user can see, with optional restrictions based on taxon and whether the set is public
     * or private
     * 
     * @param privateOnly only return private sets owned by the user or private sets shared with the user
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon (can be null)
     * @param sharedPublicOnly if true, the only public sets returned will be those that are owned by the user or have
     *        been shared with the user. If param privateOnly is true, this will have no effect.
     * @return
     */
    @Override
    public Collection<GeneSet> getUsersGeneGroups( boolean privateOnly, Long taxonId, boolean sharedPublicOnly ) {

        Taxon tax = null;
        if ( taxonId != null ) {
            tax = taxonService.load( taxonId );
            if ( tax == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
        }

        Collection<GeneSet> geneSets = new LinkedList<GeneSet>();
        
        if ( privateOnly ) {
            // gets all groups user can see (includes: owned by user, shared with user & public)
            geneSets = loadAll( tax );
            
            // this filtering is to filter out public sets
            try {
                if ( !geneSets.isEmpty() ) {
                    geneSets.retainAll( securityService.choosePrivate( geneSets ) );
                }
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        }else if ( sharedPublicOnly ){
            // gets all groups shared with the user and all groups owned by the user, except public ones
            geneSets = loadMySharedGeneSets( tax );
        }else{
            geneSets = loadAll( tax );
        }

        return geneSets;
    }

    /**
     * Returns all the gene sets user can see, with optional restrictions based on taxon and whether the set is public
     * or private
     * 
     * @param privateOnly
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return
     */
    @Override
    public Collection<DatabaseBackedGeneSetValueObject> getUsersGeneGroupsValueObjects( boolean privateOnly,
            Long taxonId ) {
        Collection<GeneSet> geneSets = getUsersGeneGroups( privateOnly, taxonId, false );
        return geneSetValueObjectHelper.convertToValueObjects( geneSets );
    }

    /**
     * Get the gene value objects for the members of the group param
     * 
     * @param groupId
     * @return
     */
    @Override
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        Collection<GeneValueObject> results = null;

        GeneSet gs = load( groupId );
        if ( gs == null ) return null; // FIXME: Send and error code/feedback?

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    /**
     * @param query string to match to gene sets
     * @param taxonId
     * @return collection of GeneSetValueObjects that match name query
     */
    @Override
    public Collection<GeneSetValueObject> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSetValueObject>();
        }
        Collection<GeneSet> foundGeneSets = null;
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
                if ( goSet != null ) foundGeneSets.add( goSet );
            } else {
                foundGeneSets.addAll( geneSetSearch.findByGoTermName( query, tax ) );
            }
        }

        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        // gsvos.addAll( DatabaseBackedGeneSetValueObject.convert2ValueObjects( foundGeneSets, false ) );
        gsvos.addAll( geneSetValueObjectHelper.convertToValueObjects( foundGeneSets ) );
        return gsvos;
    }

    /**
     * get the taxon for the gene set parameter, assumes that the taxon of the first gene will be representational of
     * all the genes
     * 
     * @param geneSetVos
     * @return the taxon or null if the gene set param was null
     */
    @Override
    public TaxonValueObject getTaxonVOforGeneSetVO( GeneSetValueObject geneSetVO ) {

        if ( geneSetVO == null ) return null;

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

    /**
     * get the taxon for the gene set parameter, assumes that the taxon of the first gene will be representational of
     * all the genes
     * 
     * @param geneSet
     * @return the taxon or null if the gene set param was null
     */
    @Override
    public Taxon getTaxonForGeneSet( GeneSet geneSet ) {
        if ( geneSet == null ) return null;
        Taxon tmpTax = null;
        tmpTax = geneSetDao.getTaxon( geneSet.getId() );
        // check top-level parent
        while ( tmpTax != null && tmpTax.getParentTaxon() != null ) {
            tmpTax = tmpTax.getParentTaxon();
        }
        return tmpTax;
    }
}
