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

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet
 * @versio n$Id$
 * @author Paul
 */
public class ExpressionAnalysisResultSetDaoImpl extends
        ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDaoBase {
    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)
     */
    @Override
    protected void handleThaw( final ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet ) {

        this.getHibernateTemplate().execute( new HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {
                session.lock( resultSet, LockMode.NONE );
                for ( ExperimentalFactor factor : resultSet.getExperimentalFactor() ) {
                    Hibernate.initialize( factor );
                }

                for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {
                    Hibernate.initialize( result );
                    if ( result instanceof ProbeAnalysisResult ) {
                        Hibernate.initialize( ( ( ProbeAnalysisResult ) result ).getProbe() );
                    }
                }
                return null;
            }
        } );

    }

}