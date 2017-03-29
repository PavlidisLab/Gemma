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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet</code>.
 * </p>
 *
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
public abstract class ExpressionExperimentSubSetDaoBase extends HibernateDaoSupport
        implements ExpressionExperimentSubSetDao {

    @Override
    public Collection<? extends ExpressionExperimentSubSet> create(
            final Collection<? extends ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate()
                .executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( ExpressionExperimentSubSet entity : entities ) {
                            create( entity );
                        }
                        return null;
                    }
                } );
        return entities;
    }


    @Override
    public ExpressionExperimentSubSet create( final ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.create - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSubSet );
        return expressionExperimentSubSet;
    }


    @Override
    public ExpressionExperimentSubSet load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate()
                .get( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl.class, id );
        return ( ExpressionExperimentSubSet ) entity;
    }


    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<ExpressionExperimentSubSet> loadAll() {
        final java.util.Collection<? extends ExpressionExperimentSubSet> results = this.getHibernateTemplate()
                .loadAll( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl.class );
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

    @Override
    public void remove( java.util.Collection<? extends ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.remove - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSubSet );
    }

    @Override
    public void update( final java.util.Collection<? extends ExpressionExperimentSubSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSubSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate()
                .executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( ExpressionExperimentSubSet entity : entities ) {
                            update( entity );
                        }
                        return null;
                    }
                } );
    }

    @Override
    public void update( ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        if ( expressionExperimentSubSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSubSet.update - 'expressionExperimentSubSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSubSet );
    }

}