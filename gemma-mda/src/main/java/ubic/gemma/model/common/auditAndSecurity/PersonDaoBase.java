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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.Person</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.Person
 */
public abstract class PersonDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.auditAndSecurity.PersonDao {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#create(int, java.util.Collection)
     */

    public java.util.Collection<? extends Person> create( final int transform,
            final java.util.Collection<? extends Person> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Person.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Person> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }
    
    
    @Override
    public Collection<? extends Person > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from PersonImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public Person create( final int transform, final ubic.gemma.model.common.auditAndSecurity.Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.create - 'person' can not be null" );
        }
        this.getHibernateTemplate().save( person );
        return ( Person ) this.transformEntity( transform, person );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#create(java.util.Collection)
     */

    @Override
    public java.util.Collection<? extends Person> create( final java.util.Collection<? extends Person> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#create(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    public Person create( ubic.gemma.model.common.auditAndSecurity.Person person ) {
        return this.create( TRANSFORM_NONE, person );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFirstAndLastName(int, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFirstAndLastName( final int transform, final java.lang.String name,
            final java.lang.String secondName ) {
        return this
                .findByFirstAndLastName(
                        transform,
                        "from ubic.gemma.model.common.auditAndSecurity.Person as person where person.name = :name and person.secondName = :secondName",
                        name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFirstAndLastName(int, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    
    @Override
    public java.util.Collection<Person> findByFirstAndLastName( final int transform,
            final java.lang.String queryString, final java.lang.String name, final java.lang.String secondName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFirstAndLastName(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFirstAndLastName( java.lang.String name, java.lang.String secondName ) {
        return this.findByFirstAndLastName( TRANSFORM_NONE, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFirstAndLastName(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFirstAndLastName( final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        return this.findByFirstAndLastName( TRANSFORM_NONE, queryString, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFullName(int, java.lang.String, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFullName( final int transform, final java.lang.String name,
            final java.lang.String secondName ) {
        return this.findByFullName( transform,
                "from PersonImpl p where p.firstName=:firstName and p.lastName=:lastName and p.middleName=:middleName",
                name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFullName(int, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    
    @Override
    public java.util.Collection<Person> findByFullName( final int transform, final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFullName(java.lang.String, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFullName( java.lang.String name, java.lang.String secondName ) {
        return this.findByFullName( TRANSFORM_NONE, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFullName(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFullName( final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        return this.findByFullName( TRANSFORM_NONE, queryString, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByLastName(int, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByLastName( final int transform, final java.lang.String lastName ) {
        return this.findByLastName( transform,
                "from ubic.gemma.model.common.auditAndSecurity.Person as person where person.lastName = :lastName",
                lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByLastName(int, java.lang.String, java.lang.String)
     */
    
    @Override
    public java.util.Collection<Person> findByLastName( final int transform, final java.lang.String queryString,
            final java.lang.String lastName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( lastName );
        argNames.add( "lastName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByLastName(java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByLastName( java.lang.String lastName ) {
        return this.findByLastName( TRANSFORM_NONE, lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByLastName(java.lang.String, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByLastName( final java.lang.String queryString,
            final java.lang.String lastName ) {
        return this.findByLastName( TRANSFORM_NONE, queryString, lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#load(int, java.lang.Long)
     */

    public Person load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Person.load - 'id' can not be null" );
        }
        final Person entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.auditAndSecurity.PersonImpl.class, id );
        return ( Person ) transformEntity( transform, entity );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#load(java.lang.Long)
     */

    @Override
    public Person load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#loadAll()
     */
    @Override
    public java.util.Collection<? extends Person> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#loadAll(int)
     */

    
    public java.util.Collection<? extends Person> loadAll( final int transform ) {
        final java.util.Collection<? extends Person> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.auditAndSecurity.PersonImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Person.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.Person entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends Person> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Person.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#remove(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    public void remove( ubic.gemma.model.common.auditAndSecurity.Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.remove - 'person' can not be null" );
        }
        this.getHibernateTemplate().delete( person );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends Person> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Person.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Person> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#update(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    public void update( ubic.gemma.model.common.auditAndSecurity.Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.update - 'person' can not be null" );
        }
        this.getHibernateTemplate().update( person );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.Person)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.auditAndSecurity.PersonDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.Person)
     */

    protected void transformEntities( final int transform, final java.util.Collection<? extends Person> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.auditAndSecurity.PersonDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.auditAndSecurity.PersonDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final Object entity ) {
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