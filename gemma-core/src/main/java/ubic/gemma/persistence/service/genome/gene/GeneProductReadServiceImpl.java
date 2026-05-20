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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.Collection;

/**
 * Implementation of {@link GeneProductReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is
 * the responsibility of the facade {@link GeneProductService} interface -- this class
 * is unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see GeneProductService
 */
@Service("geneProductReadService")
public class GeneProductReadServiceImpl implements GeneProductReadService {

    private final GeneProductDao geneProductDao;

    @Autowired
    public GeneProductReadServiceImpl( GeneProductDao geneProductDao ) {
        this.geneProductDao = geneProductDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByName( String search ) {
        return this.geneProductDao.getGenesByName( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByNcbiId( String search ) {
        return this.geneProductDao.getGenesByNcbiId( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneProduct> findByName( String name, Taxon taxon ) {
        return this.geneProductDao.findByName( name, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneProduct thaw( GeneProduct existing ) {
        return this.geneProductDao.thaw( existing );
    }
}
