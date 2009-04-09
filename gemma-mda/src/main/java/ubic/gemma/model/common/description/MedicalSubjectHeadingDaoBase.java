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
package ubic.gemma.model.common.description;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.MedicalSubjectHeading</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.MedicalSubjectHeading
 */
public abstract class MedicalSubjectHeadingDaoBase extends ubic.gemma.model.common.description.BibRefAnnotationDaoImpl
        implements ubic.gemma.model.common.description.MedicalSubjectHeadingDao {

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "MedicalSubjectHeading.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform,
                                    ( ubic.gemma.model.common.description.MedicalSubjectHeading ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#create(int transform,
     *      ubic.gemma.model.common.description.MedicalSubjectHeading)
     */
    public Object create( final int transform,
            final ubic.gemma.model.common.description.MedicalSubjectHeading medicalSubjectHeading ) {
        if ( medicalSubjectHeading == null ) {
            throw new IllegalArgumentException(
                    "MedicalSubjectHeading.create - 'medicalSubjectHeading' can not be null" );
        }
        this.getHibernateTemplate().save( medicalSubjectHeading );
        return this.transformEntity( transform, medicalSubjectHeading );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#create(ubic.gemma.model.common.description.MedicalSubjectHeading)
     */
    public ubic.gemma.model.common.description.BibRefAnnotation create(
            ubic.gemma.model.common.description.MedicalSubjectHeading medicalSubjectHeading ) {
        return ( ubic.gemma.model.common.description.MedicalSubjectHeading ) this.create( TRANSFORM_NONE,
                medicalSubjectHeading );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#load(int, java.lang.Long)
     */
    @Override
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "MedicalSubjectHeading.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.MedicalSubjectHeadingImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.MedicalSubjectHeading ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.description.BibRefAnnotation load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.MedicalSubjectHeading ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#loadAll()
     */
    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#loadAll(int)
     */
    @Override
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.MedicalSubjectHeadingImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "MedicalSubjectHeading.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.MedicalSubjectHeading entity = ( ubic.gemma.model.common.description.MedicalSubjectHeading ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.BibRefAnnotationDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "MedicalSubjectHeading.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#remove(ubic.gemma.model.common.description.MedicalSubjectHeading)
     */
    public void remove( ubic.gemma.model.common.description.MedicalSubjectHeading medicalSubjectHeading ) {
        if ( medicalSubjectHeading == null ) {
            throw new IllegalArgumentException(
                    "MedicalSubjectHeading.remove - 'medicalSubjectHeading' can not be null" );
        }
        this.getHibernateTemplate().delete( medicalSubjectHeading );
    }

    /**
     * @see ubic.gemma.model.common.description.BibRefAnnotationDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "MedicalSubjectHeading.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.MedicalSubjectHeading ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.MedicalSubjectHeadingDao#update(ubic.gemma.model.common.description.MedicalSubjectHeading)
     */
    public void update( ubic.gemma.model.common.description.MedicalSubjectHeading medicalSubjectHeading ) {
        if ( medicalSubjectHeading == null ) {
            throw new IllegalArgumentException(
                    "MedicalSubjectHeading.update - 'medicalSubjectHeading' can not be null" );
        }
        this.getHibernateTemplate().update( medicalSubjectHeading );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.MedicalSubjectHeading)} method. This method does
     * not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.description.MedicalSubjectHeadingDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.MedicalSubjectHeading)
     */
    @Override
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.description.MedicalSubjectHeadingDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.common.description.MedicalSubjectHeadingDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.description.MedicalSubjectHeading entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}