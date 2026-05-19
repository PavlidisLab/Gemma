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
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;

/**
 * Implementation of {@link BlatResultReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL
 * enforcement is the responsibility of the facade {@link BlatResultService}
 * interface -- this class is unsecured at the AOP boundary on purpose, so
 * intra-{@code gemma-core} callers can bypass duplicate ACL checks once
 * authenticated.
 *
 * @see BlatResultService
 */
@Service("blatResultReadService")
public class BlatResultReadServiceImpl implements BlatResultReadService {

    private final BlatResultDao blatResultDao;

    @Autowired
    public BlatResultReadServiceImpl( BlatResultDao blatResultDao ) {
        this.blatResultDao = blatResultDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> findByBioSequence( BioSequence bioSequence ) {
        return blatResultDao.findByBioSequence( bioSequence );
    }

    @Override
    @Transactional(readOnly = true)
    public BlatResult thaw( BlatResult blatResult ) {
        return blatResultDao.thaw( blatResult );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> thaw( Collection<BlatResult> blatResults ) {
        return blatResultDao.thaw( blatResults );
    }
}
