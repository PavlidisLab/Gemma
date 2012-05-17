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

import ubic.gemma.model.common.Auditable;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.AuditTrail</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public abstract class AuditTrailDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.auditAndSecurity.AuditTrailDao {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#addEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    public AuditEvent addEvent( final Auditable auditable, final AuditEvent auditEvent ) {
        try {
            return this.handleAddEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'AuditTrailDao.addEvent(ubic.gemma.model.common.Auditable auditable, AuditEvent auditEvent)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailDao#create(int transform, AuditTrail)
     */
    public AuditTrail create( final AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.create - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().save( auditTrail );
        return auditTrail;
    }

    /**
     * @see AuditTrailDao#create(Collection)
     */

    public Collection<? extends AuditTrail> create( final Collection<? extends AuditTrail> entities ) {
        return create( entities );
    }

    /**
     * @see AuditTrailDao#load(int, java.lang.Long)
     */
    public AuditTrail load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( AuditTrailImpl.class, id );
        return ( AuditTrail ) entity;
    }

    /**
     * @see AuditTrailDao#loadAll(int)
     */
    public Collection<? extends AuditTrail> loadAll() {
        return this.getHibernateTemplate().loadAll( AuditTrailImpl.class );
    }

    /**
     * @see AuditTrailDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'id' can not be null" );
        }
        AuditTrail entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see AuditTrailDao#remove(Collection)
     */
    public void remove( Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see AuditTrailDao#remove(AuditTrail)
     */
    public void remove( AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().delete( auditTrail );
    }

    /**
     * @see AuditTrailDao#update(Collection)
     */
    public void update( final Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'entities' can not be null" );
        }
        for ( AuditTrail auditTrail : entities ) {
            update( auditTrail );
        }
    }

    /**
     * @see AuditTrailDao#update(AuditTrail)
     */
    public void update( AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().update( auditTrail );
    }

    /**
     * Performs the core logic for {@link #addEvent(ubic.gemma.model.common.Auditable, AuditEvent)}
     */
    protected abstract AuditEvent handleAddEvent( ubic.gemma.model.common.Auditable auditable, AuditEvent auditEvent )
            throws java.lang.Exception;

}