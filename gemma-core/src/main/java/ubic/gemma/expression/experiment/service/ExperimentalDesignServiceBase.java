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
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExperimentalDesignService</code>, provides
 * access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService
 */
public abstract class ExperimentalDesignServiceBase implements ExperimentalDesignService {

    @Autowired
    private ExperimentalDesignDao experimentalDesignDao;

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#find(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesign find( final ExperimentalDesign experimentalDesign ) {
        return this.handleFind( experimentalDesign );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment getExpressionExperiment( final ExperimentalDesign experimentalDesign ) {
        return this.handleGetExpressionExperiment( experimentalDesign );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesign load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExperimentalDesign> loadAll() {
        return this.handleLoadAll();
    }

    /**
     * Sets the reference to <code>experimentalDesign</code>'s DAO.
     */
    public void setExperimentalDesignDao( ExperimentalDesignDao experimentalDesignDao ) {
        this.experimentalDesignDao = experimentalDesignDao;
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#update(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    @Transactional
    public void update( final ExperimentalDesign experimentalDesign ) {
        this.handleUpdate( experimentalDesign );
    }

    /**
     * Gets the reference to <code>experimentalDesign</code>'s DAO.
     */
    protected ExperimentalDesignDao getExperimentalDesignDao() {
        return this.experimentalDesignDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ExperimentalDesign handleFind( ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExperimentalDesign)}
     */
    protected abstract ExperimentalDesign handleFindOrCreate( ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for {@link #getExpressionExperiment(ExperimentalDesign)}
     */
    protected abstract ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign experimentalDesign );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ExperimentalDesign handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExperimentalDesign> handleLoadAll();

    /**
     * Performs the core logic for {@link #update(ExperimentalDesign)}
     */
    protected abstract void handleUpdate( ExperimentalDesign experimentalDesign );

}