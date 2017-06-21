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
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.AbstractDao;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet</code>.
 * </p>
 */
public abstract class ExpressionAnalysisResultSetDaoBase extends AbstractDao<ExpressionAnalysisResultSet>
        implements ExpressionAnalysisResultSetDao {

    public ExpressionAnalysisResultSetDaoBase(SessionFactory sessionFactory) {
        super( ExpressionAnalysisResultSet.class, sessionFactory );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thaw(ExpressionAnalysisResultSet)
     */
    @Override
    public void thaw( final ExpressionAnalysisResultSet resultSet ) {
        this.handleThaw( resultSet );
    }

    protected abstract void handleThaw( ExpressionAnalysisResultSet resultSet );

}