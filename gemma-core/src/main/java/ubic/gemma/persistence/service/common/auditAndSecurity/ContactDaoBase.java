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
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.Contact</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.auditAndSecurity.Contact
 */
public abstract class ContactDaoBase extends HibernateDaoSupport implements ContactDao {

    /**
     * @see ContactDao#create(Collection)
     */
    @Override
    public java.util.Collection<? extends Contact> create( final java.util.Collection<? extends Contact> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Contact.create - 'entities' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( Contact entity : entities ) {
                    create( entity );
                }
            }
        } );
        return entities;
    }

    /**
     * @see ContactDao#create(Object)
     */
    @Override
    public Contact create( final ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        if ( contact == null ) {
            throw new IllegalArgumentException( "Contact.create - 'contact' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().save( contact );
        return contact;
    }

    /**
     * @see ContactDao#findByEmail(String)
     */
    @Override
    public Contact findByEmail( final String email ) {
        return this.findByEmail( "from Contact c where c.email = :email", email );
    }

    private Contact findByEmail( final String queryString, final String email ) {
        java.util.List<String> argNames = new java.util.ArrayList<>();
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add( email );
        argNames.add( "email" );
        //noinspection unchecked
        java.util.Set<? extends Contact> results = new java.util.LinkedHashSet<Contact>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.auditAndSecurity.Contact"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( Contact ) result;
    }

    @Override
    public Collection<? extends Contact> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from Contact where id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    /**
     * @see ContactDao#load(Long)
     */
    @Override
    public Contact load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Contact.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate()
                .get( ubic.gemma.model.common.auditAndSecurity.Contact.class, id );
        return ( ubic.gemma.model.common.auditAndSecurity.Contact ) entity;
    }

    /**
     * @see ContactDao#loadAll()
     */
    @Override
    public java.util.Collection<? extends Contact> loadAll() {
        return this.getHibernateTemplate().loadAll( Contact.class );
    }

    /**
     * @see ContactDao#remove(Long)
     */
    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Contact.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.Contact entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( java.util.Collection<? extends Contact> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Contact.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ContactDao#remove(Object)
     */
    @Override
    public void remove( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        if ( contact == null ) {
            throw new IllegalArgumentException( "Contact.remove - 'contact' can not be null" );
        }
        this.getHibernateTemplate().delete( contact );
    }

    @Override
    public void update( final java.util.Collection<? extends Contact> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Contact.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate()
                .executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Contact entity : entities ) {
                            update( entity );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ContactDao#update(Object)
     */
    @Override
    public void update( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        if ( contact == null ) {
            throw new IllegalArgumentException( "Contact.update - 'contact' can not be null" );
        }
        this.getHibernateTemplate().update( contact );
    }

}