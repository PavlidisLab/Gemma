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

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openjena.atlas.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Paul
 */
@Repository
public class ExpressionAnalysisResultSetDaoImpl extends AbstractDao<ExpressionAnalysisResultSet>
        implements ExpressionAnalysisResultSetDao {

    private DifferentialExpressionResultDao resultDao;

    @Autowired
    public ExpressionAnalysisResultSetDaoImpl( DifferentialExpressionResultDao resultDao,
            SessionFactory sessionFactory ) {
        super( ExpressionAnalysisResultSet.class, sessionFactory );
        this.resultDao = resultDao;
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thaw(ExpressionAnalysisResultSet)
     */
    @Override
    public ExpressionAnalysisResultSet thaw( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.thawLite( resultSet );

        //noinspection unchecked
        List<ExpressionAnalysisResultSet> res = this.getSessionFactory().getCurrentSession().createQuery(
                "select r from ExpressionAnalysisResultSet r left join fetch r.results res "
                        + " left outer join fetch res.probe left join fetch res.contrasts "
                        + "inner join fetch r.experimentalFactors ef inner join fetch ef.factorValues "
                        + "where r = :rs " ).setParameter( "rs", resultSet ).list();

        assert !res.isEmpty();

        if ( timer.getTime() > 1000 ) {
            Log.info( this.getClass(), "Thaw resultSet " + res.get( 0 ).getId() + " took " + timer.getTime() + "ms" );
        }

        return res.get( 0 );
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

    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select a from GeneDifferentialExpressionMetaAnalysis a"
                        + "  inner join a.resultSetsIncluded rs where rs.analysis=:an" )
                .setParameter( "an", differentialExpressionAnalysis ).list().isEmpty();
    }

    @Override
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ( DifferentialExpressionAnalysis ) this.getSessionFactory().getCurrentSession()
                .load( DifferentialExpressionAnalysis.class, differentialExpressionAnalysis.getId() );
        Collection<ExpressionAnalysisResultSet> thawed = new HashSet<>();
        Collection<ExpressionAnalysisResultSet> rss = differentialExpressionAnalysis.getResultSets();
        int size = rss.size();
        int cnt = 0;
        for ( ExpressionAnalysisResultSet rs : rss ) {
            thawed.add( this.thaw( rs ) );
            cnt++;
            Log.info( this.getClass(), "Thawed " + cnt + "/" + size + " resultSets" );
        }
        boolean changed = differentialExpressionAnalysis.getResultSets().addAll( thawed );
        assert !changed; // they are the same objects, just updated.
        return differentialExpressionAnalysis;
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thawWithoutContrasts(ExpressionAnalysisResultSet)
     */
    @Override
    public ExpressionAnalysisResultSet thawWithoutContrasts( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.thawLite( resultSet );

        //noinspection unchecked
        List<ExpressionAnalysisResultSet> res = this.getSessionFactory().getCurrentSession().createQuery(
                "select r from ExpressionAnalysisResultSet r left join fetch r.results res "
                        + " left outer join fetch res.probe "
                        + "inner join fetch r.experimentalFactors ef inner join fetch ef.factorValues "
                        + "where r = :rs " ).setParameter( "rs", resultSet ).list();

        if ( timer.getTime() > 1000 ) {
            Log.info( this.getClass(), "Thaw resultset: " + timer.getTime() + "ms" );
        }

        assert !res.isEmpty();

        return res.get( 0 );

    }

    @Override
    public void remove( ExpressionAnalysisResultSet entity ) {
        Collection<DifferentialExpressionAnalysisResult> rss = entity.getResults();

        int size = rss.size();

        // Wipe references
        entity.setResults( new HashSet<DifferentialExpressionAnalysisResult>() );
        this.update( entity );

        // Remove results
        if ( size > 0 ) {
            AbstractDao.log.info( "Bulk removing " + size + " dea results." );
            resultDao.remove( rss );
            AbstractDao.log.info( "Done, flushing..." );
            this.getSessionFactory().getCurrentSession().flush();
        }

        // Remove result set
        AbstractDao.log.info( "Removing result set " + entity.getId() );
        super.remove( entity );
        this.getSessionFactory().getCurrentSession().flush();
    }
}