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
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Service base class for <code>ExpressionExperimentSubSetService</code>, provides access to all services and
 * entities referenced by this service.
 * 
 * @see ExpressionExperimentSubSetService
 */
public abstract class ExpressionExperimentSubSetServiceBase implements ExpressionExperimentSubSetService {

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    /**
     * @see ExpressionExperimentSubSetService#create(ExpressionExperimentSubSet)
     */
    @Override
    @Transactional
    public ExpressionExperimentSubSet create( final ExpressionExperimentSubSet expressionExperimentSubSet ) {
        return this.handleCreate( expressionExperimentSubSet );

    }

    /**
     * @see ExpressionExperimentSubSetService#load(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet load( final Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ExpressionExperimentSubSetService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * Sets the reference to <code>expressionExperimentSubSet</code>'s DAO.
     */
    public void setExpressionExperimentSubSetDao( ExpressionExperimentSubSetDao expressionExperimentSubSetDao ) {
        this.expressionExperimentSubSetDao = expressionExperimentSubSetDao;
    }

    /**
     * Gets the reference to <code>expressionExperimentSubSet</code>'s DAO.
     */
    protected ExpressionExperimentSubSetDao getExpressionExperimentSubSetDao() {
        return this.expressionExperimentSubSetDao;
    }

    /**
     * Performs the core logic for {@link #create(ExpressionExperimentSubSet)}
     */
    protected abstract ExpressionExperimentSubSet handleCreate( ExpressionExperimentSubSet expressionExperimentSubSet );

    /**
     * Performs the core logic for {@link #load(Long)}
     */
    protected abstract ExpressionExperimentSubSet handleLoad( Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExpressionExperimentSubSet> handleLoadAll();

}