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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExperimentalDesignService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService
 */
public abstract class ExperimentalDesignServiceBase implements
        ubic.gemma.model.expression.experiment.ExperimentalDesignService {

    @Autowired
    private ubic.gemma.model.expression.experiment.ExperimentalDesignDao experimentalDesignDao;

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#create(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign create(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        try {
            return this.handleCreate( experimentalDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.create(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#find(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign find(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        try {
            return this.handleFind( experimentalDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.find(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#findByName(java.lang.String)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findOrCreate(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        try {
            return this.handleFindOrCreate( experimentalDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        try {
            return this.handleGetExpressionExperiment( experimentalDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#load(java.lang.Long)
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#loadAll()
     */
    public java.util.Collection<ExperimentalDesign> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>experimentalDesign</code>'s DAO.
     */
    public void setExperimentalDesignDao(
            ubic.gemma.model.expression.experiment.ExperimentalDesignDao experimentalDesignDao ) {
        this.experimentalDesignDao = experimentalDesignDao;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#update(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    public void update( final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        try {
            this.handleUpdate( experimentalDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExperimentalDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignService.update(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>experimentalDesign</code>'s DAO.
     */
    protected ubic.gemma.model.expression.experiment.ExperimentalDesignDao getExperimentalDesignDao() {
        return this.experimentalDesignDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleCreate(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleFind(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleFindOrCreate(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleGetExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExperimentalDesign> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign )
            throws java.lang.Exception;

}