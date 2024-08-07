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

package ubic.gemma.persistence.service.association;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.cache.CacheUtils;
import ubic.gemma.persistence.service.AbstractService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author klc
 * @see    Gene2GOAssociationService
 */
@Service
public class Gene2GOAssociationServiceImpl extends AbstractService<Gene2GOAssociation>
        implements Gene2GOAssociationService, InitializingBean {

    private static final String G2G_CACHE_NAME = "Gene2GoServiceCache";
    private final Gene2GOAssociationDao gene2GOAssociationDao;
    private final CacheManager cacheManager;
    private Cache gene2goCache;

    @Autowired
    public Gene2GOAssociationServiceImpl( Gene2GOAssociationDao mainDao, CacheManager cacheManager ) {
        super( mainDao );
        this.gene2GOAssociationDao = mainDao;
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterPropertiesSet() {
        this.gene2goCache = CacheUtils.getCache( cacheManager, Gene2GOAssociationServiceImpl.G2G_CACHE_NAME );
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
    public Collection<Gene> findByGOTerms( Collection<String> termsToFetch, @Nullable Taxon taxon ) {
        if ( taxon == null ) {
            return this.gene2GOAssociationDao.findByGoTerms( termsToFetch );
        } else {
            return this.gene2GOAssociationDao.findByGoTerms( termsToFetch, taxon );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Collection<Gene>> findByGOTermsPerTaxon( Collection<String> termsToFetch ) {
        return this.gene2GOAssociationDao.findByGoTermsPerTaxon( termsToFetch );
    }

    @Override
    @Transactional
    public int removeAll() {
        return this.gene2GOAssociationDao.removeAll();
    }
}