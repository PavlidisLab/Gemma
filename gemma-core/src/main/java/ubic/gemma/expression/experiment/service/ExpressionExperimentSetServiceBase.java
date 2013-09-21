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

import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * Spring Service base class for <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSetService</code>,
 * provides access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService
 * @version $Id$
 */
public abstract class ExpressionExperimentSetServiceBase implements ExpressionExperimentSetService {

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    @Transactional
    public ExpressionExperimentSet create( final ExpressionExperimentSet expressionExperimentSet ) {
        return this.handleCreate( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    @Transactional
    public void delete( final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException( "Cannot delete null set" );
        }
        Long id = expressionExperimentSet.getId();
        if ( id == null || id < 0 ) {
            throw new IllegalArgumentException( "Cannot delete eeset with id=" + id );
        }
        if ( getAnalyses( expressionExperimentSet ).size() > 0 ) {
            throw new IllegalArgumentException( "Sorry, can't delete this set, it is associated with active analyses." );
        }

        this.handleDelete( expressionExperimentSet );

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#findByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionExperimentSet> findByName( final java.lang.String name ) {
        return this.handleFindByName( name );

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionAnalysis> getAnalyses(
            final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        return this.handleGetAnalyses( expressionExperimentSet );

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.analysis.expression.ExpressionExperimentSet load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionExperimentSet> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionExperimentSet> loadUserSets(
            final ubic.gemma.model.common.auditAndSecurity.User user ) {
        return this.handleLoadUserSets( user );

    }

    /**
     * Sets the reference to <code>expressionExperimentSet</code>'s DAO.
     */

    public void setExpressionExperimentSetDao( ExpressionExperimentSetDao expressionExperimentSetDao ) {
        this.expressionExperimentSetDao = expressionExperimentSetDao;
    }

    /**
     * Gets the reference to <code>expressionExperimentSet</code>'s DAO.
     */
    protected ExpressionExperimentSetDao getExpressionExperimentSetDao() {
        return this.expressionExperimentSetDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract ExpressionExperimentSet handleCreate( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract void handleDelete( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract java.util.Collection<ExpressionAnalysis> handleGetAnalyses(
            ExpressionExperimentSet expressionExperimentSet );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ExpressionExperimentSet handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleLoadAll();

    /**
     * Performs the core logic for {@link #loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleLoadUserSets( User user );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract void handleUpdate( ExpressionExperimentSet expressionExperimentSet );

}