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
package ubic.gemma.model.association;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>GeneHomology</code>.
 * </p>
 * 
 * @see GeneHomology
 */
public abstract class GeneHomologyDaoBase extends HibernateDaoSupport implements GeneHomologyDao {

    /**
     * @see GeneHomologyDao#create(int, Collection)
     */
    @Override
    public Collection create( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneHomology.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( GeneHomology ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends GeneHomology> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneHomologyImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see GeneHomologyDao#create(int transform, GeneHomology)
     */
    @Override
    public GeneHomology create( final GeneHomology geneHomology ) {
        if ( geneHomology == null ) {
            throw new IllegalArgumentException( "GeneHomology.create - 'geneHomology' can not be null" );
        }
        this.getHibernateTemplate().save( geneHomology );
        return geneHomology;
    }

    /**
     * @see GeneHomologyDao#load(int, java.lang.Long)
     */
    @Override
    public GeneHomology load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneHomology.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( GeneHomologyImpl.class, id );
        return ( GeneHomology ) entity;
    }

    /**
     * @see GeneHomologyDao#loadAll(int)
     */
    @Override
    public Collection loadAll() {
        final Collection results = this.getHibernateTemplate().loadAll( GeneHomologyImpl.class );

        return results;
    }

    /**
     * @see GeneHomologyDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneHomology.remove - 'id' can not be null" );
        }
        GeneHomology entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see RelationshipDao#remove(Collection)
     */
    @Override
    public void remove( Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneHomology.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see GeneHomologyDao#remove(GeneHomology)
     */
    @Override
    public void remove( GeneHomology geneHomology ) {
        if ( geneHomology == null ) {
            throw new IllegalArgumentException( "GeneHomology.remove - 'geneHomology' can not be null" );
        }
        this.getHibernateTemplate().delete( geneHomology );
    }

    /**
     * @see RelationshipDao#update(Collection)
     */
    @Override
    public void update( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneHomology.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( GeneHomology ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see GeneHomologyDao#update(GeneHomology)
     */
    @Override
    public void update( GeneHomology geneHomology ) {
        if ( geneHomology == null ) {
            throw new IllegalArgumentException( "GeneHomology.update - 'geneHomology' can not be null" );
        }
        this.getHibernateTemplate().update( geneHomology );
    }

}