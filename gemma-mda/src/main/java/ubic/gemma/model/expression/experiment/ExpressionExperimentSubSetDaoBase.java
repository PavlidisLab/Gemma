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

import ubic.gemma.model.common.auditAndSecurity.Contact;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
public abstract class ExpressionExperimentSubSetDaoBase extends BioAssaySetDaoImpl<ExpressionExperimentSubSet>
        implements ExpressionExperimentSubSetDao {

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#create(int, java.util.Collection)
     */
    @Override
    public Collection<? extends ExpressionExperimentSubSet> create(
            final Collection<? extends ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExpressionExperimentSubSet> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#create(int transform,
     *      ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public ExpressionExperimentSubSet create( final ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.create - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSubSet );
        return expressionExperimentSubSet;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#findByInvestigator(int,
     *      java.lang.String, ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    public java.util.Collection<ExpressionExperimentSubSet> findByInvestigator( final java.lang.String queryString,
            final Contact investigator ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( investigator );
        argNames.add( "investigator" );
        java.util.List<ExpressionExperimentSubSet> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#findByInvestigator(int,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    @Override
    public java.util.Collection<ExpressionExperimentSubSet> findByInvestigator( final Contact investigator ) {
        return this
                .findByInvestigator(
                        "from ExpressionExperimentSubSetImpl i inner join Contact c on c in elements(i.investigators) or c == i.owner where c == :investigator",
                        investigator );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#load(int, java.lang.Long)
     */

    @Override
    public ExpressionExperimentSubSet load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl.class, id );
        return ( ExpressionExperimentSubSet ) entity;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<ExpressionExperimentSubSet> loadAll() {
        final java.util.Collection<? extends ExpressionExperimentSubSet> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl.class );
        return ( Collection<ExpressionExperimentSubSet> ) results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#remove(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public void remove( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.remove - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSubSet );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExpressionExperimentSubSet> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao#update(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    public void update( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.update - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSubSet );
    }

}