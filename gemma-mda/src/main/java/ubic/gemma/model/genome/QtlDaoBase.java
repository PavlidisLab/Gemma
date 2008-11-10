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
package ubic.gemma.model.genome;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.Qtl</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.Qtl
 */
public abstract class QtlDaoBase extends BaseQtlDaoImpl<Qtl> implements QtlDao {

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#create(int, java.util.Collection)
     */
    public java.util.Collection<Qtl> create( final int transform, final java.util.Collection<Qtl> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Qtl.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Qtl> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.QtlDao#create(int transform, ubic.gemma.model.genome.Qtl)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.Qtl qtl ) {
        if ( qtl == null ) {
            throw new IllegalArgumentException( "Qtl.create - 'qtl' can not be null" );
        }
        this.getHibernateTemplate().save( qtl );
        return this.transformEntity( transform, qtl );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#create(ubic.gemma.model.genome.Qtl)
     */
    public Qtl create( ubic.gemma.model.genome.Qtl qtl ) {
        return ( ubic.gemma.model.genome.Qtl ) this.create( TRANSFORM_NONE, qtl );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Qtl.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.QtlImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.Qtl ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#load(java.lang.Long)
     */

    public Qtl load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.Qtl ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<Qtl> loadAll( final int transform ) {
        final java.util.Collection<Qtl> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.QtlImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Qtl.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.Qtl entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<Qtl> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Qtl.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#remove(ubic.gemma.model.genome.Qtl)
     */
    public void remove( ubic.gemma.model.genome.Qtl qtl ) {
        if ( qtl == null ) {
            throw new IllegalArgumentException( "Qtl.remove - 'qtl' can not be null" );
        }
        this.getHibernateTemplate().delete( qtl );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<Qtl> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Qtl.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Qtl> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#update(ubic.gemma.model.genome.Qtl)
     */
    public void update( ubic.gemma.model.genome.Qtl qtl ) {
        if ( qtl == null ) {
            throw new IllegalArgumentException( "Qtl.update - 'qtl' can not be null" );
        }
        this.getHibernateTemplate().update( qtl );
    }

}