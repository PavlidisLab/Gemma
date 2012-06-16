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
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.FactorValueService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.FactorValueService
 */
public abstract class FactorValueServiceBase implements ubic.gemma.model.expression.experiment.FactorValueService {

    @Autowired
    private ubic.gemma.model.expression.experiment.FactorValueDao factorValueDao;

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#create(ubic.gemma.model.expression.experiment.FactorValue)
     */
    @Override
    public ubic.gemma.model.expression.experiment.FactorValue create(
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return this.handleCreate( factorValue );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#delete(ubic.gemma.model.expression.experiment.FactorValue)
     */
    @Override
    public void delete( final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        this.handleDelete( factorValue );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#findOrCreate(ubic.gemma.model.expression.experiment.FactorValue)
     */
    @Override
    public ubic.gemma.model.expression.experiment.FactorValue findOrCreate(
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        return this.handleFindOrCreate( factorValue );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.expression.experiment.FactorValue load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#loadAll()
     */
    @Override
    public java.util.Collection<FactorValue> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * Sets the reference to <code>factorValue</code>'s DAO.
     */
    public void setFactorValueDao( ubic.gemma.model.expression.experiment.FactorValueDao factorValueDao ) {
        this.factorValueDao = factorValueDao;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<FactorValue> factorValues ) {
        this.handleUpdate( factorValues );

    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#update(ubic.gemma.model.expression.experiment.FactorValue)
     */
    @Override
    public void update( final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        this.handleUpdate( factorValue );

    }

    /**
     * Gets the reference to <code>factorValue</code>'s DAO.
     */
    protected ubic.gemma.model.expression.experiment.FactorValueDao getFactorValueDao() {
        return this.factorValueDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.experiment.FactorValue)}
     */
    protected abstract ubic.gemma.model.expression.experiment.FactorValue handleCreate(
            ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.expression.experiment.FactorValue)}
     */
    protected abstract void handleDelete( ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.FactorValue)}
     */
    protected abstract ubic.gemma.model.expression.experiment.FactorValue handleFindOrCreate(
            ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.experiment.FactorValue handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<FactorValue> handleLoadAll();

    /**
     * Performs the core logic for {@link #update(java.util.Collection)}
     */
    protected abstract void handleUpdate( java.util.Collection<FactorValue> factorValues );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.experiment.FactorValue)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.experiment.FactorValue factorValue );

}