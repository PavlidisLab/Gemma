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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;

/**
 * Implementation of {@link ExperimentalDesignReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL
 * enforcement is the responsibility of the facade
 * {@link ExperimentalDesignService} interface -- this class is unsecured at
 * the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 *
 * @see ExperimentalDesignService
 */
@Service("experimentalDesignReadService")
public class ExperimentalDesignReadServiceImpl implements ExperimentalDesignReadService {

    private final ExperimentalDesignDao experimentalDesignDao;

    @Autowired
    public ExperimentalDesignReadServiceImpl( ExperimentalDesignDao experimentalDesignDao ) {
        this.experimentalDesignDao = experimentalDesignDao;
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesign loadWithExperimentalFactors( Long id ) {
        ExperimentalDesign ed = experimentalDesignDao.load( id );
        if ( ed != null ) {
            ed.getExperimentalFactors().forEach( Hibernate::initialize );
        }
        return ed;
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesign getRandomExperimentalDesignThatNeedsAttention( ExperimentalDesign excludedDesign ) {
        return experimentalDesignDao.getRandomExperimentalDesignThatNeedsAttention( excludedDesign );
    }
}
