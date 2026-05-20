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
package ubic.gemma.persistence.service.genome.biosequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of {@link BioSequenceReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link BioSequenceService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see BioSequenceService
 */
@Service("bioSequenceReadService")
public class BioSequenceReadServiceImpl implements BioSequenceReadService {

    private final BioSequenceDao bioSequenceDao;

    @Autowired
    public BioSequenceReadServiceImpl( BioSequenceDao bioSequenceDao ) {
        this.bioSequenceDao = bioSequenceDao;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public BioSequence findByAccession( DatabaseEntry accession ) {
        return this.bioSequenceDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Gene, Collection<BioSequence>> findByGenes( Collection<Gene> genes ) {
        return this.bioSequenceDao.findByGenes( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioSequence> findByName( String name ) {
        return this.bioSequenceDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByAccession( String search ) {
        return this.bioSequenceDao.getGenesByAccession( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByName( String search ) {
        return this.bioSequenceDao.getGenesByName( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioSequence> thaw( Collection<BioSequence> bioSequences ) {
        return this.bioSequenceDao.thaw( bioSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public BioSequence thaw( BioSequence bioSequence ) {
        return this.bioSequenceDao.thaw( bioSequence );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public BioSequence findByCompositeSequence( CompositeSequence compositeSequence ) {
        return this.bioSequenceDao.findByCompositeSequence( compositeSequence );
    }
}
