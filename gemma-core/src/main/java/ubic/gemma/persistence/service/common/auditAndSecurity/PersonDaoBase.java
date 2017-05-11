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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.jdbc.Work;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.auditAndSecurity.Person;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.Person</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.auditAndSecurity.Person
 */
public abstract class PersonDaoBase extends HibernateDaoSupport implements PersonDao {

    /**
     * @see PersonDao#create(java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Person> create( final java.util.Collection<? extends Person> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Person.create - 'entities' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().doWork( new Work() {

            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( Person entity : entities ) {
                    create( entity );
                }
            }
        } );

        return entities;
    }

    /**
     * @see PersonDao#create(Object)
     */
    @Override
    public Person create( final Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.create - 'person' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().save( person );
        return person;
    }

    /**
     * @see PersonDao#findByFullName(String, String)
     */
    @Override
    public java.util.Collection<Person> findByFullName( final String name, final String secondName ) {
        return this.findByFullName(
                "from Person p where p.firstName=:firstName and p.lastName=:lastName and p.middleName=:middleName",
                name, secondName );
    }

    /**
     * @see PersonDao#findByFullName(String, String)
     */
    @SuppressWarnings("unchecked")
    private java.util.Collection<Person> findByFullName( final String queryString, final String name,
            final String secondName ) {
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "name", name )
                .setParameter( "secondName", secondName ).list();
    }

    /**
     * @see PersonDao#findByLastName(String)
     */
    @Override
    public java.util.Collection<Person> findByLastName( final String lastName ) {
        return this.findByLastName(
                "from ubic.gemma.model.common.auditAndSecurity.Person as person where person.lastName = :lastName",
                lastName );
    }

    /**
     * @see PersonDao#findByLastName(String)
     */
    @SuppressWarnings("unchecked")
    private Collection<Person> findByLastName( final String queryString, final String lastName ) {
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "lastName", lastName ).list();
    }

    @Override
    public Collection<? extends Person> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from Person where id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    /**
     * @see PersonDao#load(Long)
     */
    @Override
    public Person load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Person.load - 'id' can not be null" );
        }
        return ( Person ) this.getSessionFactory().getCurrentSession().get( Person.class, id );
    }

    /**
     * @see PersonDao#loadAll()
     */

    @Override
    public java.util.Collection<? extends Person> loadAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( Person.class ).list();
    }

    /**
     * @see PersonDao#remove(Long)
     */

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Person.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.Person entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( Collection<? extends Person> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Person.remove - 'entities' can not be null" );
        }
        for ( Person p : entities ) {
            this.remove( p );
        }
    }

    /**
     * @see PersonDao#remove(Object)
     */
    @Override
    public void remove( Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.remove - 'person' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().delete( person );
    }

    @Override
    public void update( final java.util.Collection<? extends Person> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Person.update - 'entities' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( Person entity : entities ) {
                    update( entity );
                }
            }
        } );
    }

    /**
     * @see PersonDao#update(Object)
     */
    @Override
    public void update( Person person ) {
        if ( person == null ) {
            throw new IllegalArgumentException( "Person.update - 'person' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().update( person );
    }

}