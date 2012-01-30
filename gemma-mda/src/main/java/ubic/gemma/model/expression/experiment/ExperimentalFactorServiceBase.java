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
public abstract class ExperimentalFactorServiceBase implements
        ubic.gemma.model.expression.experiment.ExperimentalFactorService {

    @Autowired
    private ubic.gemma.model.expression.experiment.ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#create(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalFactor create(
            final ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        try {
            return this.handleCreate( experimentalFactor );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.create(ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    public void delete( final ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        try {
            this.handleDelete( experimentalFactor );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.delete(ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#find(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalFactor find(
            final ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        try {
            return this.handleFind( experimentalFactor );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.find(ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalFactor findOrCreate(
            final ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        try {
            return this.handleFindOrCreate( experimentalFactor );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#load(java.lang.Long)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalFactor load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#loadAll()
     */
    public java.util.Collection<ExperimentalFactor> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#update(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    public void update( final ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        try {
            this.handleUpdate( experimentalFactor );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalFactorServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalFactorService.update(ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor)' --> "
                            + th, th );
        }
    }

    protected DifferentialExpressionAnalysisDao getDifferentialExpressionAnalysisDao() {
        return differentialExpressionAnalysisDao;
    }

    /**
     * Gets the reference to <code>experimentalFactor</code>'s DAO.
     */
    protected ubic.gemma.model.expression.experiment.ExperimentalFactorDao getExperimentalFactorDao() {
        return this.experimentalFactorDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalFactor handleCreate(
            ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract void handleDelete( ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalFactor handleFind(
            ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalFactor handleFindOrCreate(
            ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalFactor handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExperimentalFactor> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor )
            throws java.lang.Exception;

}