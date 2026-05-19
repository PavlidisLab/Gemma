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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;

import java.util.Collection;

/**
 * Implementation of {@link BlatAssociationReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL
 * enforcement is the responsibility of the facade
 * {@link BlatAssociationService} interface -- this class is unsecured at the
 * AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 *
 * @see BlatAssociationService
 */
@Service("blatAssociationReadService")
public class BlatAssociationReadServiceImpl implements BlatAssociationReadService {

    private final BlatAssociationDao blatAssociationDao;

    @Autowired
    public BlatAssociationReadServiceImpl( BlatAssociationDao blatAssociationDao ) {
        this.blatAssociationDao = blatAssociationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BlatAssociation> find( BioSequence bioSequence ) {
        return blatAssociationDao.find( bioSequence );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BlatAssociation> find( Gene gene ) {
        return blatAssociationDao.find( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BlatAssociation> findAndThaw( BioSequence bioSequence ) {
        Collection<BlatAssociation> results = blatAssociationDao.find( bioSequence );
        blatAssociationDao.thaw( results );
        return results;
    }
}
