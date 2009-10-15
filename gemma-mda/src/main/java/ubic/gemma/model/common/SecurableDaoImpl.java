/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.model.common;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.security.acl.AclEntry;
import org.springframework.security.acl.basic.AclObjectIdentity;
import org.springframework.security.acl.basic.NamedEntityObjectIdentity;
import org.springframework.security.acl.basic.SimpleAclEntry;

import ubic.gemma.util.EntityUtils;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.Securable
 */
public class SecurableDaoImpl<T extends Securable> extends ubic.gemma.model.common.SecurableDaoBase<T> {
    protected Log log = LogFactory.getLog( getClass().getName() );

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.SecurableDaoBase#getAclObjectIdentityId(ubic.gemma.model.common.Securable)
     */
    public Long getAclObjectIdentityId( Securable target ) {

        String objectIdentity = createObjectIdentityFromObject( target );

        String queryString = "SELECT id FROM acl_object_identity WHERE object_identity = ?";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setParameter( 0, objectIdentity );
            Integer result = ( Integer ) queryObject.uniqueResult();

            Long longId = null;
            if ( result != null ) {
                longId = new Long( result );
            }

            return longId;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<AclEntry> getAclEntries( final Securable target ) {
        Long oid = this.getAclObjectIdentityId( target );
        final AclObjectIdentity aoi = getObjectIdentity( target );
        final AclObjectIdentity parentOi = getParentObjectIdentity( target );
        final String queryString = "SELECT a.recipient, a.mask FROM acl_permission a inner join acl_object_identity o ON o.id=a.acl_object_identity WHERE o.object_identity = ?";

        return ( Collection<AclEntry> ) this.getHibernateTemplate().execute( new HibernateCallback() {

            public Object doInHibernate( Session session ) throws HibernateException, SQLException {
                Query q = session.createSQLQuery( queryString );
                q.setParameter( 0, createObjectIdentityFromObject( target ) );
                List list = q.list();

                Collection<AclEntry> results = new HashSet<AclEntry>();
                for ( Object object : list ) {
                    Object[] oa = ( Object[] ) object;
                    String recipient = ( String ) oa[0];
                    Integer mask = ( Integer ) oa[1];
                    AclEntry ae = new SimpleAclEntry( recipient, aoi, parentOi, mask );
                    results.add( ae );
                }

                return results;
            }
        } );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.SecurableDao#getAclObjectIdentityParent(ubic.gemma.model.common.Securable)
     */
    public Integer getAclObjectIdentityParentId( Securable target ) {

        String objectIdentity = createObjectIdentityFromObject( target );

        String queryString = "select parent_object from acl_object_identity where object_identity = ?";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setParameter( 0, objectIdentity );
            Integer result = ( Integer ) queryObject.uniqueResult();

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * Creates the object_identity to be used in the acl_object_identity table.
     * 
     * @param target
     * @return
     */
    private String createObjectIdentityFromObject( Securable target ) {

        Securable implementation = ( Securable ) EntityUtils.getImplementationForProxy( target );
        if (implementation == null){
            log.warn( "No Implemntation returned for proxy: " + target );
            return null;
        }
        
        Long id = implementation.getId();

        return implementation.getClass().getName() + ":" + id;
    }

    /**
     * @param target
     * @return AclObjectIdentity representing the target.
     */
    private AclObjectIdentity getObjectIdentity( Securable target ) {
        Securable implementation = ( Securable ) EntityUtils.getImplementationForProxy( target );
        Long id = implementation.getId();
        return new NamedEntityObjectIdentity( implementation.getClass().getName(), id.toString() );
    }

    /**
     * @param target
     * @return
     */
    @SuppressWarnings("deprecation")
    private AclObjectIdentity getParentObjectIdentity( Securable target ) {
        Integer id = this.getAclObjectIdentityParentId( target );

        if ( id == null ) {
            return null;
        }

        String queryString = "select object_identity from acl_object_identity where id = :id";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setParameter( "id", id );

            Object o = queryObject.uniqueResult();

            String oi = ( String ) o;

            return new NamedEntityObjectIdentity( oi.substring( 0, oi.indexOf( ':' ) ), oi.substring(
                    oi.indexOf( ':' ) + 1, oi.length() ) );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }
}