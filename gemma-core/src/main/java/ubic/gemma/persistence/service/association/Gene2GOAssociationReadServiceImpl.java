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
package ubic.gemma.persistence.service.association;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.cache.CacheUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Implementation of {@link Gene2GOAssociationReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link Gene2GOAssociationService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 * <p>
 * Owns the {@code Gene2GoServiceCache} ehcache region used by {@link #findByGene(Gene)} and
 * {@link #findByGenes(Collection)} (population by gene-key, with per-gene fallback to the
 * DAO on cache miss).
 *
 * @see Gene2GOAssociationService
 */
@Service("gene2GOAssociationReadService")
public class Gene2GOAssociationReadServiceImpl implements Gene2GOAssociationReadService, InitializingBean {

    private static final String G2G_CACHE_NAME = "Gene2GoServiceCache";

    private final Gene2GOAssociationDao gene2GOAssociationDao;
    private final CacheManager cacheManager;
    private Cache gene2goCache;

    @Autowired
    public Gene2GOAssociationReadServiceImpl( Gene2GOAssociationDao gene2GOAssociationDao, CacheManager cacheManager ) {
        this.gene2GOAssociationDao = gene2GOAssociationDao;
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterPropertiesSet() {
        this.gene2goCache = CacheUtils.getCache( cacheManager, G2G_CACHE_NAME );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene2GOAssociation> findAssociationByGene( Gene gene ) {
        return this.gene2GOAssociationDao.findAssociationByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene2GOAssociation> findAssociationByGenes( Collection<Gene> genes ) {
        return gene2GOAssociationDao.findAssociationByGenes( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByGene( Gene gene ) {

        Cache.ValueWrapper element = this.gene2goCache.get( gene );

        if ( element != null ) //noinspection unchecked
            return ( Collection<Characteristic> ) element.get();

        Collection<Characteristic> re = this.gene2GOAssociationDao.findByGene( gene );

        this.gene2goCache.put( gene, re );

        return re;

    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<Gene, Collection<Characteristic>> findByGenes( Collection<Gene> genes ) {
        Map<Gene, Collection<Characteristic>> result = new HashMap<>();

        Collection<Gene> needToFind = new HashSet<>();
        for ( Gene gene : genes ) {
            Cache.ValueWrapper element = this.gene2goCache.get( gene );

            if ( element != null )
                result.put( gene, ( Collection<Characteristic> ) element.get() );
            else
                needToFind.add( gene );
        }

        result.putAll( this.gene2GOAssociationDao.findByGenes( needToFind ) );

        return result;

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByGOTermUris( Collection<String> uris, @Nullable Taxon taxon ) {
        if ( taxon == null ) {
            return this.gene2GOAssociationDao.findByGoTermUris( uris );
        } else {
            return this.gene2GOAssociationDao.findByGoTermUris( uris, taxon );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Collection<Gene>> findByGOTermUrisPerTaxon( Collection<String> uris ) {
        return this.gene2GOAssociationDao.findByGoTermUrisPerTaxon( uris );
    }
}
