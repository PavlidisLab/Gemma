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
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ProteinProteinInteraction</code>.
 * </p>
 * 
 * @see ProteinProteinInteraction
 */
public abstract class ProteinProteinInteractionDaoBase extends HibernateDaoSupport implements
        ProteinProteinInteractionDao {

    /**
     * @see ProteinProteinInteractionDao#create(int, Collection)
     */
    public Collection create( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ProteinProteinInteraction ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    public Collection<? extends ProteinProteinInteraction> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProteinProteinInteractionImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ProteinProteinInteractionDao#create(int transform, ProteinProteinInteraction)
     */
    public ProteinProteinInteraction create( final ProteinProteinInteraction proteinProteinInteraction ) {
        if ( proteinProteinInteraction == null ) {
            throw new IllegalArgumentException(
                    "ProteinProteinInteraction.create - 'proteinProteinInteraction' can not be null" );
        }
        this.getHibernateTemplate().save( proteinProteinInteraction );
        return proteinProteinInteraction;
    }

    /**
     * @see ProteinProteinInteractionDao#load(int, java.lang.Long)
     */
    public ProteinProteinInteraction load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ProteinProteinInteractionImpl.class, id );
        return ( ProteinProteinInteraction ) entity;
    }

    /**
     * @see ProteinProteinInteractionDao#loadAll(int)
     */
    public Collection loadAll() {
        final Collection results = this.getHibernateTemplate().loadAll( ProteinProteinInteractionImpl.class );

        return results;
    }

    /**
     * @see ProteinProteinInteractionDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.remove - 'id' can not be null" );
        }
        ProteinProteinInteraction entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see RelationshipDao#remove(Collection)
     */
    public void remove( Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ProteinProteinInteractionDao#remove(ProteinProteinInteraction)
     */
    public void remove( ProteinProteinInteraction proteinProteinInteraction ) {
        if ( proteinProteinInteraction == null ) {
            throw new IllegalArgumentException(
                    "ProteinProteinInteraction.remove - 'proteinProteinInteraction' can not be null" );
        }
        this.getHibernateTemplate().delete( proteinProteinInteraction );
    }

    /**
     * @see RelationshipDao#update(Collection)
     */
    public void update( final Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ProteinProteinInteraction ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ProteinProteinInteractionDao#update(ProteinProteinInteraction)
     */
    public void update( ProteinProteinInteraction proteinProteinInteraction ) {
        if ( proteinProteinInteraction == null ) {
            throw new IllegalArgumentException(
                    "ProteinProteinInteraction.update - 'proteinProteinInteraction' can not be null" );
        }
        this.getHibernateTemplate().update( proteinProteinInteraction );
    }

}