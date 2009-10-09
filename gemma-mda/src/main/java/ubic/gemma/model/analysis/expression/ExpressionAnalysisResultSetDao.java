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
package ubic.gemma.model.analysis.expression;

import java.util.Collection;

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet
 */
public interface ExpressionAnalysisResultSetDao extends BaseDao<ExpressionAnalysisResultSet> {

    public ExpressionAnalysisResultSet create( ExpressionAnalysisResultSet expressionAnalysisResultSet );

    public Collection<ExpressionAnalysisResultSet> create( Collection<ExpressionAnalysisResultSet> resultSets );

    /**
     * Loads an instance of ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet from the persistent store.
     */
    public ExpressionAnalysisResultSet load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection<ExpressionAnalysisResultSet> loadAll();

    /**
     * Removes the instance of ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet having the given
     * <code>identifier</code> from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection<ExpressionAnalysisResultSet> entities );

    /**
     * Removes the instance of ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet from the persistent
     * store.
     */
    public void remove( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet expressionAnalysisResultSet );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection<ExpressionAnalysisResultSet> entities );

    /**
     * Updates the <code>expressionAnalysisResultSet</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet expressionAnalysisResultSet );

    /**
     * @param resultSet Only thaws the factor not the probe information
     */
    public void thawLite( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet );

}
