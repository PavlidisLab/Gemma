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

    @Override
    public java.util.Collection<? extends Person> create( final java.util.Collection<? extends Person> entities ) {
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
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends Person> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from PersonImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    public Person create( final Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.create - 'person' can not be null" );
        }
        this.getHibernateTemplate().save( person );
        return person;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFullName(int, java.lang.String, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByFullName( final java.lang.String name, final java.lang.String secondName ) {
        return this.findByFullName(
                "from PersonImpl p where p.firstName=:firstName and p.lastName=:lastName and p.middleName=:middleName",
                name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByFullName(int, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public java.util.Collection<Person> findByFullName( final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Person> ) results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByLastName(int, java.lang.String)
     */
    @Override
    public java.util.Collection<Person> findByLastName( final java.lang.String lastName ) {
        return this.findByLastName(
                "from ubic.gemma.model.common.auditAndSecurity.Person as person where person.lastName = :lastName",
                lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#findByLastName(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public Collection<Person> findByLastName( final java.lang.String queryString, final java.lang.String lastName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( lastName );
        argNames.add( "lastName" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Person> ) results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#load(int, java.lang.Long)
     */

    @Override
    public Person load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Person.load - 'id' can not be null" );
        }
        final Person entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.auditAndSecurity.PersonImpl.class, id );
        return entity;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends Person> loadAll() {
        final java.util.Collection<? extends Person> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.auditAndSecurity.PersonImpl.class );
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

}