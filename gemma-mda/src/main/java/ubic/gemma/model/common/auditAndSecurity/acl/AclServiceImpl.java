/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.model.common.auditAndSecurity.acl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.ChildrenExistException;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ubic.gemma.model.common.auditAndSecurity.Infrastructure;

/**
 * Not a service because we expect some methods to be only called from an existing transaction, and to avoid
 * double-proxying.
 * <p>
 * The read-only methods are transactional, but start their own transactions. This is necessary for methods that are
 * called outside a transaction, namely method security interception.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Infrastructure
public class AclServiceImpl implements AclService, InitializingBean {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog( AclServiceImpl.class.getName() );

    @Autowired
    private AclDao aclDao;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void afterPropertiesSet() throws Exception {
        aclDao.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.acls.model.MutableAclService#createAcl(org.springframework.security.acls.model.
     * ObjectIdentity)
     */
    @Override
    public MutableAcl createAcl( ObjectIdentity objectIdentity ) throws AlreadyExistsException {
        // Check this object identity hasn't already been persisted
        if ( find( objectIdentity ) != null ) {
            throw new AlreadyExistsException( "Object identity '" + objectIdentity + "' already exists in the database" );
        }

        // Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AclPrincipalSid sid = new AclPrincipalSid( ( String ) auth.getPrincipal() );

        // Create the acl_object_identity row
        objectIdentity = createObjectIdentity( objectIdentity, sid );

        Acl acl = this.readAclById( objectIdentity );

        return ( MutableAcl ) acl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.acls.model.MutableAclService#deleteAcl(org.springframework.security.acls.model.
     * ObjectIdentity, boolean)
     */
    @Override
    public void deleteAcl( ObjectIdentity objectIdentity, boolean deleteChildren ) throws ChildrenExistException {
        aclDao.delete( find( objectIdentity ), deleteChildren );
    }

    /**
     * Remove a sid and all associated ACEs.
     * 
     * @param sid
     */
    @Override
    public void deleteSid( Sid sid ) {
        aclDao.delete( sid );
    }

    @Override
    public List<ObjectIdentity> findChildren( ObjectIdentity parentIdentity ) {
        return aclDao.findChildren( parentIdentity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.security.acls.model.AclService#readAclById(org.springframework.security.acls.model.ObjectIdentity
     * )
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Acl readAclById( ObjectIdentity object ) throws NotFoundException {
        return readAclById( object, null );
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Acl readAclById( ObjectIdentity object, List<Sid> sids ) throws NotFoundException {
        Map<ObjectIdentity, Acl> map = readAclsById( Arrays.asList( object ), sids );
        return map.get( object );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.acls.model.AclService#readAclsById(java.util.List)
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects ) throws NotFoundException {
        return readAclsById( objects, null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.acls.model.AclService#readAclsById(java.util.List, java.util.List)
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects, List<Sid> sids )
            throws NotFoundException {

        // deals with cache.
        Map<ObjectIdentity, Acl> result = aclDao.readAclsById( objects, sids );

        // Check every requested object identity was found (throw NotFoundException if needed)
        for ( int i = 0; i < objects.size(); i++ ) {
            ObjectIdentity key = objects.get( i );

            // has to match the type we are using in the db...otherwise we don't get the match.
            // / AclObjectIdentity aoi = new AclObjectIdentity( key.getType(), key.getIdentifier() );

            if ( !result.containsKey( key ) ) {
                throw new NotFoundException( "Unable to find ACL information for object identity '" + key + "'" );
            }

            assert result.get( key ) != null;
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.acls.model.MutableAclService#updateAcl(org.springframework.security.acls.model.
     * MutableAcl)
     */
    @Override
    public MutableAcl updateAcl( MutableAcl acl ) throws NotFoundException {
        Assert.notNull( acl.getId(), "Object Identity doesn't provide an identifier" );
        aclDao.update( acl );
        return acl;
    }

    /**
     * Persist
     * 
     * @param object
     * @param owner
     * @return persistent objectIdentity (will be an AclObjectIdentity)
     */
    protected AclObjectIdentity createObjectIdentity( ObjectIdentity object, Sid owner ) {
        Sid sid = createOrRetrieveSid( owner, true );
        String type = object.getType();
        return aclDao.createObjectIdentity( type, object.getIdentifier(), sid, Boolean.TRUE );
    }

    /**
     * Retrieves the primary key from acl_sid, creating a new row if needed and the allowCreate property is true.
     * 
     * @param sid to find or create
     * @param allowCreate true if creation is permitted if not found
     * @return the primary key or null if not found
     * @throws IllegalArgumentException if the <tt>Sid</tt> is not a recognized implementation.
     */
    protected Sid createOrRetrieveSid( Sid sid, boolean allowCreate ) {
        if ( allowCreate ) {
            return aclDao.findOrCreate( sid );
        }
        return aclDao.find( sid );

    }

    private ObjectIdentity find( ObjectIdentity oid ) {
        AclObjectIdentity acloi = aclDao.find( oid );
        return acloi;
    }
}
