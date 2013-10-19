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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet
 * @versio n$Id$
 * @author Paul
 */
@Repository
public class ExpressionAnalysisResultSetDaoImpl extends
        ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetDaoBase {

    @Autowired
    public ExpressionAnalysisResultSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetDao#canDelete(ubic.gemma.model.analysis.
     * expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        final String queryString = "select a from GeneDifferentialExpressionMetaAnalysisImpl a"
                + "  inner join a.resultSetsIncluded rs where rs.analysis=:an";

        List<GeneDifferentialExpressionMetaAnalysis> qresult = this.getHibernateTemplate().findByNamedParam(
                queryString, "an", differentialExpressionAnalysis );
        return qresult.isEmpty();
    }

    @Override
    public void thawLite( final ExpressionAnalysisResultSet resultSet ) {

        Session session = this.getSessionFactory().getCurrentSession();

        session.buildLockRequest( LockOptions.NONE ).lock( resultSet );
        for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
            Hibernate.initialize( factor );
        }

        Hibernate.initialize( resultSet.getAnalysis() );
        Hibernate.initialize( resultSet.getAnalysis().getExperimentAnalyzed() );

    }

    /**
     * @see ExpressionAnalysisResultSetDao#thaw(ExpressionAnalysisResultSet)
     */
    @Override
    protected ExpressionAnalysisResultSet handleThaw( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.thawLite( resultSet );

        List<ExpressionAnalysisResultSet> res = this.getHibernateTemplate().findByNamedParam(
                "select r from ExpressionAnalysisResultSetImpl r left join fetch r.results res "
                        + " left outer join fetch res.probe left join fetch res.contrasts "
                        + "inner join fetch r.experimentalFactors ef inner join fetch ef.factorValues "
                        + "where r = :rs ", "rs", resultSet );

        if ( timer.getTime() > 1000 ) {
            Log.info( "Thaw resultset: " + timer.getTime() + "ms" );
        }

        assert !res.isEmpty();

        return res.get( 0 );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetDao#thawFully(ubic.gemma.model.analysis.
     * expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ( DifferentialExpressionAnalysis ) this.getSessionFactory()
                .getCurrentSession()
                .load( DifferentialExpressionAnalysisImpl.class, differentialExpressionAnalysis.getId() );
        // Hibernate.initialize( differentialExpressionAnalysis );
        // Hibernate.initialize( differentialExpressionAnalysis.getResultSets() );
        Collection<ExpressionAnalysisResultSet> thawed = new HashSet<ExpressionAnalysisResultSet>();
        for ( ExpressionAnalysisResultSet rs : differentialExpressionAnalysis.getResultSets() ) {
            thawed.add( this.thaw( rs ) );
        }
        boolean changed = differentialExpressionAnalysis.getResultSets().addAll( thawed );
        assert !changed; // they are the same objects, just updated.
        return differentialExpressionAnalysis;
    }
}