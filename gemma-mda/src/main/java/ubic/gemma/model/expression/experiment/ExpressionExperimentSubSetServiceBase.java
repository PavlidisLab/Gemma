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
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService
 */
public abstract class ExpressionExperimentSubSetServiceBase implements
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService {

    @Autowired
    private ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#create(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet create(
            final ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        try {
            return this.handleCreate( expressionExperimentSubSet );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService.create(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#loadAll()
     */
    @Override
    public java.util.Collection loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>expressionExperimentSubSet</code>'s DAO.
     */
    public void setExpressionExperimentSubSetDao(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao expressionExperimentSubSetDao ) {
        this.expressionExperimentSubSetDao = expressionExperimentSubSetDao;
    }

    /**
     * Gets the reference to <code>expressionExperimentSubSet</code>'s DAO.
     */
    protected ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao getExpressionExperimentSubSetDao() {
        return this.expressionExperimentSubSetDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet handleCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection handleLoadAll() throws java.lang.Exception;

}