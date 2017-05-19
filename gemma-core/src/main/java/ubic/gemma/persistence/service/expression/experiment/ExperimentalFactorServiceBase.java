/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.experiment;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * <p>
 * Spring Service base class for <code>ExperimentalFactorService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ExperimentalFactorService
 */
public abstract class ExperimentalFactorServiceBase implements ExperimentalFactorService {

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    /**
     * @see ExperimentalFactorService#delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    @Transactional
    public void delete( final ExperimentalFactor experimentalFactor ) {
        this.handleDelete( experimentalFactor );

    }

    /**
     * @see ExperimentalFactorService#find(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    @Transactional(readOnly = true)
    public ExperimentalFactor find( final ExperimentalFactor experimentalFactor ) {
        return this.handleFind( experimentalFactor );

    }

    /**
     * @see ExperimentalFactorService#findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    @Transactional
    public ExperimentalFactor findOrCreate( final ExperimentalFactor experimentalFactor ) {
        return this.handleFindOrCreate( experimentalFactor );

    }

    /**
     * @see ExperimentalFactorService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ExperimentalFactor load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * @see ExperimentalFactorService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentalFactor> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * @see ExperimentalFactorService#update(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    @Transactional
    public void update( final ExperimentalFactor experimentalFactor ) {
        this.handleUpdate( experimentalFactor );

    }

    DifferentialExpressionAnalysisDao getDifferentialExpressionAnalysisDao() {
        return differentialExpressionAnalysisDao;
    }

    /**
     * Gets the reference to <code>experimentalFactor</code>'s DAO.
     */
    ExperimentalFactorDao getExperimentalFactorDao() {
        return this.experimentalFactorDao;
    }

    protected abstract ExperimentalFactor handleCreate( ExperimentalFactor experimentalFactor );

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract void handleDelete( ExperimentalFactor experimentalFactor );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract ExperimentalFactor handleFind( ExperimentalFactor experimentalFactor );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract ExperimentalFactor handleFindOrCreate( ExperimentalFactor experimentalFactor );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ExperimentalFactor handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract Collection<ExperimentalFactor> handleLoadAll();

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract void handleUpdate( ExperimentalFactor experimentalFactor );

}