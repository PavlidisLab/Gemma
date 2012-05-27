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
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExperimentalFactor</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 */
public abstract class ExperimentalFactorDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.expression.experiment.ExperimentalFactorDao {

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends ExperimentalFactor> create(
            final java.util.Collection<? extends ExperimentalFactor> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExperimentalFactor> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorDao#create(int transform,
     *      ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor create( final ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.create - 'experimentalFactor' can not be null" );
        }
        this.getHibernateTemplate().saveOrUpdate( experimentalFactor );
        return experimentalFactor;
    }

    @Override
    public Collection<? extends ExperimentalFactor> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExperimentalFactorImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorDao#load(int, java.lang.Long)
     */

    @Override
    public ExperimentalFactor load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.experiment.ExperimentalFactorImpl.class, id );
        return ( ubic.gemma.model.expression.experiment.ExperimentalFactor ) entity;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends ExperimentalFactor> loadAll() {
        final java.util.Collection<? extends ExperimentalFactor> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.experiment.ExperimentalFactorImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.experiment.ExperimentalFactor entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends ExperimentalFactor> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Object)
     */
    @Override
    public void remove( ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.remove - 'experimentalFactor' can not be null" );
        }
        this.getHibernateTemplate().delete( experimentalFactor );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends ExperimentalFactor> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExperimentalFactor> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorDao#update(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public void update( ubic.gemma.model.expression.experiment.ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null ) {
            throw new IllegalArgumentException( "ExperimentalFactor.update - 'experimentalFactor' can not be null" );
        }
        this.getHibernateTemplate().update( experimentalFactor );
    }

}