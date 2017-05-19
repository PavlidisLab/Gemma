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
package ubic.gemma.persistence.service.expression.biomaterial;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.expression.biomaterial.Compound;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.biomaterial.Compound</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.biomaterial.Compound
 */
public abstract class CompoundDaoBase extends HibernateDaoSupport implements CompoundDao {

    /**
     * @see CompoundDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Compound> create( final java.util.Collection<? extends Compound> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Compound.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Compound> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see CompoundDao#create(int transform,
     *      ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public Compound create( final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        if ( compound == null ) {
            throw new IllegalArgumentException( "Compound.create - 'compound' can not be null" );
        }
        this.getHibernateTemplate().save( compound );
        return compound;
    }

    @Override
    public Collection<? extends Compound> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from CompoundImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see CompoundDao#load(int, java.lang.Long)
     */

    @Override
    public Compound load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Compound.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.biomaterial.CompoundImpl.class, id );
        return ( Compound ) entity;
    }

    /**
     * @see CompoundDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<Compound> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.biomaterial.CompoundImpl.class );
        return ( Collection<Compound> ) results;
    }

    /**
     * @see CompoundDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Compound.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.biomaterial.Compound entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends Compound> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Compound.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see CompoundDao#remove(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public void remove( ubic.gemma.model.expression.biomaterial.Compound compound ) {
        if ( compound == null ) {
            throw new IllegalArgumentException( "Compound.remove - 'compound' can not be null" );
        }
        this.getHibernateTemplate().delete( compound );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends Compound> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Compound.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Compound> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see CompoundDao#update(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public void update( ubic.gemma.model.expression.biomaterial.Compound compound ) {
        if ( compound == null ) {
            throw new IllegalArgumentException( "Compound.update - 'compound' can not be null" );
        }
        this.getHibernateTemplate().update( compound );
    }

}