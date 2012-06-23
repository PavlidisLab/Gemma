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
package ubic.gemma.expression.experiment.service;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExperimentalDesignService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService
 */
public abstract class ExperimentalDesignServiceBase implements
        ubic.gemma.expression.experiment.service.ExperimentalDesignService {

    @Autowired
    private ubic.gemma.model.expression.experiment.ExperimentalDesignDao experimentalDesignDao;

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#create(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExperimentalDesign create(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return this.handleCreate( experimentalDesign );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#find(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExperimentalDesign find(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return this.handleFind( experimentalDesign );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#findByName(java.lang.String)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findByName( final java.lang.String name ) {
        return this.handleFindByName( name );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findOrCreate(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return this.handleFindOrCreate( experimentalDesign );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment(
            final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        return this.handleGetExpressionExperiment( experimentalDesign );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExperimentalDesign load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#loadAll()
     */
    @Override
    public java.util.Collection<ExperimentalDesign> loadAll() {
        return this.handleLoadAll();
    }

    /**
     * Sets the reference to <code>experimentalDesign</code>'s DAO.
     */
    public void setExperimentalDesignDao(
            ubic.gemma.model.expression.experiment.ExperimentalDesignDao experimentalDesignDao ) {
        this.experimentalDesignDao = experimentalDesignDao;
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#update(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public void update( final ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        this.handleUpdate( experimentalDesign );
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
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleFind(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleFindOrCreate(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleGetExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExperimentalDesign handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExperimentalDesign> handleLoadAll();

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

}