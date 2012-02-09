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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;

/**
 * @see ubic.gemma.model.analysis.ExpressionExperimentSet
 * @version $Id$
 * @author paul
 */
@Repository
public class ExpressionExperimentSetDaoImpl extends ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase {

    @Autowired
    public ExpressionExperimentSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#find(ubic.gemma.model.expression.experiment.
     * BioAssaySet)
     */
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees from ExpressionExperimentSetImpl ees inner join ees.experiments e where e = :ee", "ee",
                bioAssaySet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#getExperimentsInSet(java.lang.Long)
     */
    @Override
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees.experiments from ExpressionExperimentSetImpl ees where ees.id = :id", "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAllMultiExperimentSets()
     */
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getHibernateTemplate().find(
                "select ees from ExpressionExperimentSetImpl ees where size(ees.experiments) > 1" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAllExperimentSetsWithTaxon()
     */
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.getHibernateTemplate().find(
                "select ees from ExpressionExperimentSetImpl ees where ees.taxon is not null" );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#thaw(ubic.gemma.model.analysis.expression.
     * ExpressionExperimentSet)
     */
    @Override
    public void thaw( final ExpressionExperimentSet expressionExperimentSet ) {

        this.getHibernateTemplate().execute( new HibernateCallback<Object>() {

            @Override
            public Object doInHibernate( Session session ) throws HibernateException, SQLException {
                session.buildLockRequest( LockOptions.NONE ).lock( expressionExperimentSet );
                Hibernate.initialize( expressionExperimentSet );
                Hibernate.initialize( expressionExperimentSet.getTaxon() );
                Hibernate.initialize( expressionExperimentSet.getExperiments() );
                return null;
            }
        } );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleFindByName(java.lang.String)
     */
    @Override
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where name=:query",
                "query", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleGetAnalyses(ubic.gemma.model.analysis
     * .expression.ExpressionExperimentSet)
     */
    @Override
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws Exception {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select a from ExpressionAnalysisImpl a inner join a.expressionExperimentSetAnalyzed ees where ees = :eeset ",
                        "eeset", expressionExperimentSet );
    }

    @Override
    public int getExperimentCount( Long id ) {

        List<?> o = this.getHibernateTemplate().findByNamedParam(
                "select e.id, count(i) from ExpressionExperimentSetImpl e join e.experiments i where e.id in (:ids)",
                "ids", id );

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            return ( ( Long ) oa[1] ).intValue();
        }
        
        return 0;

    }

}