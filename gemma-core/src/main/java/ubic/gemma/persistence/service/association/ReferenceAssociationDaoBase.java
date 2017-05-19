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
package ubic.gemma.persistence.service.association;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.association.ReferenceAssociation;
import ubic.gemma.model.association.ReferenceAssociationImpl;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ReferenceAssociation</code>.
 * 
 * @see ReferenceAssociation
 */
public abstract class ReferenceAssociationDaoBase extends HibernateDaoSupport implements ReferenceAssociationDao {

    /**
     * @see ReferenceAssociationDao#create(int, Collection)
     */
    @Override
    public Collection<? extends ReferenceAssociation> create( final Collection<? extends ReferenceAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends ReferenceAssociation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ReferenceAssociationDao#create(int transform, ReferenceAssociation)
     */
    @Override
    public ReferenceAssociation create( final ReferenceAssociation referenceAssociation ) {
        if ( referenceAssociation == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.create - 'referenceAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( referenceAssociation );
        return referenceAssociation;
    }

    @Override
    public Collection<? extends ReferenceAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ReferenceAssociationImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ReferenceAssociationDao#load(int, java.lang.Long)
     */

    @Override
    public ReferenceAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ReferenceAssociationImpl.class, id );
        return ( ReferenceAssociation ) entity;
    }

    /**
     * @see ReferenceAssociationDao#loadAll(int)
     */

    @Override
    public Collection<? extends ReferenceAssociation> loadAll() {
        return this.getHibernateTemplate().loadAll( ReferenceAssociationImpl.class );
    }

    /**
     * @see RelationshipDao#remove(Collection)
     */

    @Override
    public void remove( Collection<? extends ReferenceAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ReferenceAssociationDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.remove - 'id' can not be null" );
        }
        ReferenceAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ReferenceAssociationDao#remove(ReferenceAssociation)
     */
    @Override
    public void remove( ReferenceAssociation referenceAssociation ) {
        if ( referenceAssociation == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.remove - 'referenceAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( referenceAssociation );
    }

    /**
     * @see RelationshipDao#update(Collection)
     */

    @Override
    public void update( final Collection<? extends ReferenceAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends ReferenceAssociation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ReferenceAssociationDao#update(ReferenceAssociation)
     */
    @Override
    public void update( ReferenceAssociation referenceAssociation ) {
        if ( referenceAssociation == null ) {
            throw new IllegalArgumentException( "ReferenceAssociation.update - 'referenceAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( referenceAssociation );
    }

}