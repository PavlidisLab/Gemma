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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.ProteinProteinInteraction</code>.
 * </p>
 * 
 * @see ubic.gemma.model.association.ProteinProteinInteraction
 */
public abstract class ProteinProteinInteractionDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.association.ProteinProteinInteractionDao {

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.association.ProteinProteinInteraction ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }
    
    
    public Collection<? extends ProteinProteinInteraction > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProteinProteinInteractionImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#create(int transform,
     *      ubic.gemma.model.association.ProteinProteinInteraction)
     */
    public ProteinProteinInteraction create(
            final ubic.gemma.model.association.ProteinProteinInteraction proteinProteinInteraction ) {
        if ( proteinProteinInteraction == null ) {
            throw new IllegalArgumentException(
                    "ProteinProteinInteraction.create - 'proteinProteinInteraction' can not be null" );
        }
        this.getHibernateTemplate().save( proteinProteinInteraction );
        return proteinProteinInteraction;
    }

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#load(int, java.lang.Long)
     */
    public ProteinProteinInteraction load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.association.ProteinProteinInteractionImpl.class, id );
        return ( ubic.gemma.model.association.ProteinProteinInteraction ) entity;
    }

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#loadAll(int)
     */
    public java.util.Collection loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.association.ProteinProteinInteractionImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.remove - 'id' can not be null" );
        }
        ubic.gemma.model.association.ProteinProteinInteraction entity = ( ubic.gemma.model.association.ProteinProteinInteraction ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#remove(ubic.gemma.model.association.ProteinProteinInteraction)
     */
    public void remove( ubic.gemma.model.association.ProteinProteinInteraction proteinProteinInteraction ) {
        if ( proteinProteinInteraction == null ) {
            throw new IllegalArgumentException(
                    "ProteinProteinInteraction.remove - 'proteinProteinInteraction' can not be null" );
        }
        this.getHibernateTemplate().delete( proteinProteinInteraction );
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProteinProteinInteraction.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.association.ProteinProteinInteraction ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.association.ProteinProteinInteractionDao#update(ubic.gemma.model.association.ProteinProteinInteraction)
     */
    public void update( ubic.gemma.model.association.ProteinProteinInteraction proteinProteinInteraction ) {
        if ( proteinProteinInteraction == null ) {
            throw new IllegalArgumentException(
                    "ProteinProteinInteraction.update - 'proteinProteinInteraction' can not be null" );
        }
        this.getHibernateTemplate().update( proteinProteinInteraction );
    }

}