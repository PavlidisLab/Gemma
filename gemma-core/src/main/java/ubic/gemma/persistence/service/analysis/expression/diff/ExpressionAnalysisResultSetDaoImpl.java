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
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Paul
 */
@Repository
public class ExpressionAnalysisResultSetDaoImpl extends ExpressionAnalysisResultSetDaoBase {

    @Autowired
    public ExpressionAnalysisResultSetDaoImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.getSession().createQuery( "select a from GeneDifferentialExpressionMetaAnalysisImpl a"
                + "  inner join a.resultSetsIncluded rs where rs.analysis=:an" ).list().isEmpty();
    }

    @Override
    public void thawLite( final ExpressionAnalysisResultSet resultSet ) {

        Session session = this.getSession();

        session.buildLockRequest( LockOptions.NONE ).lock( resultSet );
        for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
            Hibernate.initialize( factor );
        }

        Hibernate.initialize( resultSet.getAnalysis() );
        System.out.println( "thawing experiment analyzed for rs: "+resultSet.getId() );
        Hibernate.initialize( resultSet.getAnalysis().getExperimentAnalyzed() );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thawWithoutContrasts(ExpressionAnalysisResultSet)
     */
    @Override
    public void thawWithoutContrasts( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.thawLite( resultSet );

        Collection<DifferentialExpressionAnalysisResult> rss = resultSet.getResults();
        Hibernate.initialize( rss );
        for(DifferentialExpressionAnalysisResult rs : rss){
            Hibernate.initialize( rs.getProbe() );
        }
        Collection<ExperimentalFactor> efs = resultSet.getExperimentalFactors();
        Hibernate.initialize( efs );
        for(ExperimentalFactor ef : efs){
            Hibernate.initialize( ef.getFactorValues() );
        }

        if ( timer.getTime() > 1000 ) {
            Log.info( "Thaw result set: " + timer.getTime() + "ms" );
        }

    }

    @Override
    public void thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ( DifferentialExpressionAnalysis ) this.getSession()
                .load( DifferentialExpressionAnalysis.class, differentialExpressionAnalysis.getId() );
        Collection<ExpressionAnalysisResultSet> thawed = new HashSet<>();
        for ( ExpressionAnalysisResultSet rs : differentialExpressionAnalysis.getResultSets() ) {
            this.thaw( rs );
            thawed.add( rs );
        }
        boolean changed = differentialExpressionAnalysis.getResultSets().addAll( thawed );
        assert !changed; // they are the same objects, just updated.
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thaw(ExpressionAnalysisResultSet)
     */
    @Override
    protected void handleThaw( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.getSession().refresh( resultSet );
        this.thawLite( resultSet );

        Hibernate.initialize( resultSet.getResults() );
        for ( DifferentialExpressionAnalysisResult dear : resultSet.getResults() ) {
            Hibernate.initialize( dear.getProbe() );
            Hibernate.initialize( dear.getContrasts() );
        }
        Hibernate.initialize( resultSet.getExperimentalFactors() );
        for ( ExperimentalFactor ef : resultSet.getExperimentalFactors() ) {
            Hibernate.initialize( ef.getFactorValues() );
        }

        if ( timer.getTime() > 1000 ) {
            Log.info( "Thaw result set: " + timer.getTime() + "ms" );
        }

    }
}