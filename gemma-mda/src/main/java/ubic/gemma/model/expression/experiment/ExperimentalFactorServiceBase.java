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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExperimentalFactorService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService
 */
public abstract class ExperimentalFactorServiceBase implements ExperimentalFactorService {

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#create(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor create( final ExperimentalFactor experimentalFactor ) {
        return this.handleCreate( experimentalFactor );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public void delete( final ExperimentalFactor experimentalFactor ) {
        this.handleDelete( experimentalFactor );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#find(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor find( final ExperimentalFactor experimentalFactor ) {
        return this.handleFind( experimentalFactor );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor findOrCreate( final ExperimentalFactor experimentalFactor ) {
        return this.handleFindOrCreate( experimentalFactor );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#load(java.lang.Long)
     */
    @Override
    public ExperimentalFactor load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#loadAll()
     */
    @Override
    public Collection<ExperimentalFactor> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#update(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public void update( final ExperimentalFactor experimentalFactor ) {
        this.handleUpdate( experimentalFactor );

    }

    protected DifferentialExpressionAnalysisDao getDifferentialExpressionAnalysisDao() {
        return differentialExpressionAnalysisDao;
    }

    /**
     * Gets the reference to <code>experimentalFactor</code>'s DAO.
     */
    protected ExperimentalFactorDao getExperimentalFactorDao() {
        return this.experimentalFactorDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
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