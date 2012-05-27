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
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>LiteratureAssociation</code>.
 * 
 * @see LiteratureAssociation
 */
public abstract class LiteratureAssociationDaoBase extends HibernateDaoSupport implements LiteratureAssociationDao {

    /**
     * @see LiteratureAssociationDao#create(int, Collection)
     */
    @Override
    public Collection create( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "LiteratureAssociation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( LiteratureAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends LiteratureAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from LiteratureAssociationImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see LiteratureAssociationDao#create(int transform, LiteratureAssociation)
     */
    @Override
    public LiteratureAssociation create( final LiteratureAssociation literatureAssociation ) {
        if ( literatureAssociation == null ) {
            throw new IllegalArgumentException(
                    "LiteratureAssociation.create - 'literatureAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( literatureAssociation );
        return literatureAssociation;
    }

    /**
     * @see LiteratureAssociationDao#load(int, java.lang.Long)
     */
    @Override
    public LiteratureAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "LiteratureAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( LiteratureAssociationImpl.class, id );
        return ( LiteratureAssociation ) entity;
    }

    /**
     * @see LiteratureAssociationDao#loadAll(int)
     */
    @Override
    public Collection loadAll() {
        final Collection results = this.getHibernateTemplate().loadAll( LiteratureAssociationImpl.class );

        return results;
    }

    /**
     * @see LiteratureAssociationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "LiteratureAssociation.remove - 'id' can not be null" );
        }
        LiteratureAssociation entity = this.load( id );
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
            throw new IllegalArgumentException( "LiteratureAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see LiteratureAssociationDao#remove(LiteratureAssociation)
     */
    @Override
    public void remove( LiteratureAssociation literatureAssociation ) {
        if ( literatureAssociation == null ) {
            throw new IllegalArgumentException(
                    "LiteratureAssociation.remove - 'literatureAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( literatureAssociation );
    }

    /**
     * @see RelationshipDao#update(Collection)
     */
    @Override
    public void update( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "LiteratureAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( LiteratureAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see LiteratureAssociationDao#update(LiteratureAssociation)
     */
    @Override
    public void update( LiteratureAssociation literatureAssociation ) {
        if ( literatureAssociation == null ) {
            throw new IllegalArgumentException(
                    "LiteratureAssociation.update - 'literatureAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( literatureAssociation );
    }

}