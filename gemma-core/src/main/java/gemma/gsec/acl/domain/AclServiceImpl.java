/*
 * The Gemma project
 *
 * Copyright (c) 2010-2013 University of British Columbia
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
package gemma.gsec.acl.domain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author paul
 * @version $Id: AclServiceImpl.java,v 1.1 2013/09/14 16:55:19 paul Exp $
 */
public class AclServiceImpl implements AclService {

    private static final Log log = LogFactory.getLog( AclServiceImpl.class );

    private final AclDao aclDao;

    public AclServiceImpl( AclDao aclDao ) {
        this.aclDao = aclDao;
    }

    @Override
    @Transactional
    public MutableAcl createAcl( ObjectIdentity objectIdentity ) throws AlreadyExistsException {
        // Check this object identity hasn't already been persisted
        AclObjectIdentity aoi;
        if ( ( aoi = aclDao.find( objectIdentity ) ) != null ) {
            throw new AlreadyExistsException( "There is already an object identity for  " + objectIdentity + " in the database: " + aoi + "." );
        }

        // Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AclSid sid = aclDao.findOrCreate( new AclPrincipalSid( auth ) );

        // Create the acl_object_identity row
        String type = objectIdentity.getType();
        aoi = aclDao.createObjectIdentity( type, objectIdentity.getIdentifier(), sid, true );

        Acl acl = aclDao.readAclsById( Collections.singletonList( aoi ) ).get( aoi );

        return ( MutableAcl ) acl;
    }

    @Override
    @Transactional
    public void deleteAcl( ObjectIdentity objectIdentity, boolean deleteChildren ) throws ChildrenExistException {
        AclObjectIdentity aclObjectIdentity = aclDao.find( objectIdentity );
        if ( aclObjectIdentity != null ) {
            aclDao.delete( aclObjectIdentity, deleteChildren );
        } else {
            log.warn( "Unable to find ACL object identity for " + objectIdentity + ". No exception will be raised since this method is attempting to delete it." );
        }
    }

    @Override
    @Transactional
    public void deleteSid( Sid sid ) {
        AclSid aclSid = aclDao.find( sid );
        if ( aclSid != null ) {
            aclDao.delete( aclSid );
        } else {
            log.warn( "Unable to find ACL sid for " + sid + ". No exception will be raised since this method is attempting to delete it." );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectIdentity> findChildren( final ObjectIdentity parentIdentity ) {
        AclObjectIdentity aclParentIdentity = getAclObject( parentIdentity );
        return new ArrayList<>( aclDao.findChildren( aclParentIdentity ) );
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = NotFoundException.class)
    public Acl readAclById( ObjectIdentity object ) throws NotFoundException {
        AclObjectIdentity aclObject = getAclObject( object );
        Acl acl = aclDao.readAclsById( Collections.singletonList( aclObject ) ).get( aclObject );
        if ( acl == null ) {
            throw new NotFoundException( "Unable to find ACL information for " + aclObject + "." );
        }
        return acl;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = NotFoundException.class)
    public Acl readAclById( ObjectIdentity object, List<Sid> sids ) throws NotFoundException {
        Acl acl = aclDao.readAclsById( Collections.singletonList( getAclObject( object ) ) )
            .get( getAclObject( object ) );
        if ( acl == null ) {
            throw new NotFoundException( "Unable to find ACL information for " + getAclObject( object ) + "." );
        }
        return acl;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = NotFoundException.class)
    public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects ) throws NotFoundException {
        if ( objects.isEmpty() ) {
            return Collections.emptyMap();
        }
        List<AclObjectIdentity> aclObjects = getAclObjects( objects );
        Map<AclObjectIdentity, Acl> results = aclDao.readAclsById( aclObjects );
        if ( results.isEmpty() ) {
            throw new NotFoundException( "Unable to find ACL information for all of the requested objects." );
        }
        return new HashMap<>( results );
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = NotFoundException.class)
    public Map<ObjectIdentity, Acl> readAclsById( final List<ObjectIdentity> objects, final List<Sid> sids ) throws NotFoundException {
        if ( objects.isEmpty() ) {
            return Collections.emptyMap();
        }
        List<AclObjectIdentity> aclObjects = getAclObjects( objects );
        Map<AclObjectIdentity, Acl> results = aclDao.readAclsById( aclObjects );
        if ( results.isEmpty() ) {
            throw new NotFoundException( "Unable to find ACL information for all of the requested objects." );
        }
        return new HashMap<>( results );
    }

    @Override
    public Acl readAclById( ObjectIdentity objectIdentity, Session session ) throws NotFoundException {
        AclObjectIdentity aclObject = aclDao.find( objectIdentity, session );
        if ( aclObject == null ) {
            throw new NotFoundException( "Unable to find ACL information for " + objectIdentity + "." );
        }
        Acl acl = aclDao.readAclsById( Collections.singletonList( aclObject ), session ).get( aclObject );
        if ( acl == null ) {
            throw new NotFoundException( "Unable to find ACL object identity for " + objectIdentity + "." );
        }
        return acl;
    }

    @Override
    // this is only used for reading ACLs now
    @Transactional(readOnly = true)
    public Session openSession() {
        return aclDao.openSession();
    }

    @Override
    @Transactional
    public MutableAcl updateAcl( final MutableAcl acl ) throws NotFoundException {
        Assert.notNull( acl.getId(), "Object Identity doesn't provide an identifier" );
        aclDao.update( acl );
        return acl;
    }

    private List<AclObjectIdentity> getAclObjects( List<ObjectIdentity> objects ) throws NotFoundException {
        Assert.notEmpty( objects, "objects must not be empty" );
        ArrayList<AclObjectIdentity> aclObjects = new ArrayList<>( objects.size() );
        for ( ObjectIdentity object : objects ) {
            AclObjectIdentity aclObjectIdentity = aclDao.find( object );
            if ( aclObjectIdentity != null ) {
                aclObjects.add( aclObjectIdentity );
            }
        }
        return aclObjects;
    }

    private AclObjectIdentity getAclObject( ObjectIdentity oi ) throws NotFoundException {
        AclObjectIdentity result = aclDao.find( oi );
        if ( result == null ) {
            throw new NotFoundException( "Unable to find ACL object identity for " + oi + "." );
        }
        return result;
    }
}
