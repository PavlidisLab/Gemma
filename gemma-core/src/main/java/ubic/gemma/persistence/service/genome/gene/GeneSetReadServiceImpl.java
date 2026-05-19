/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.genome.gene;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.SessionBoundGeneSetValueObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link GeneSetReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link GeneSetService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated. Callers that need {@code @PostFilter}-style
 * permission filtering on the result collection (the {@code loadWithMembers},
 * {@code findByGene}, {@code findByName}, {@code loadAll}, and {@code loadMy*} reads)
 * MUST inject {@link GeneSetService} (the facade) instead of this read service.
 *
 * @see GeneSetService
 */
@Service("geneSetReadService")
@Slf4j
public class GeneSetReadServiceImpl implements GeneSetReadService {

    private final GeneSetDao geneSetDao;
    private final GeneService geneService;

    @Autowired
    public GeneSetReadServiceImpl( GeneSetDao geneSetDao, GeneService geneService ) {
        this.geneSetDao = geneSetDao;
        this.geneService = geneService;
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
    public DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id ) {
        return geneSetDao.loadValueObjectByIdLite( id );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> geneSetIds ) {
        return this.geneSetDao.loadValueObjectsByIdsLite( geneSetIds );
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

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> getGenesInGroup( GeneSetValueObject object ) {
        GeneSet gs = this.geneSetDao.load( object.getId() );
        if ( gs == null )
            return null;
        return GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getGeneIdsInGroup( GeneSetValueObject object ) {
        DatabaseBackedGeneSetValueObject vo = this.geneSetDao.loadValueObjectById( object.getId() );
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
}
