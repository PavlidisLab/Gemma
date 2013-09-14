/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.security.authorization.acl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecureValueObject;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.SecurityUtil;

/**
 * Security check for reading value objects.
 * <p>
 * As a side effect, it fills in security status information in the value objects, but only if permission was granted.
 * 
 * @author cmcdonald
 * @version $Id$
 */
public class AclAfterFilterValueObjectCollectionProvider extends AbstractAclProvider {

    protected static final Log logger = LogFactory.getLog( AclAfterFilterValueObjectCollectionProvider.class );

    /**
     * @param aclService
     * @param requirePermission
     */
    public AclAfterFilterValueObjectCollectionProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ", requirePermission );
        this.setObjectIdentityRetrievalStrategy( new ValueObjectAwareIdentityRetrievalStrategyImpl() );

    }

    @Autowired
    private SecurityService securityService;

    @Override
    @SuppressWarnings("unchecked")
    public final Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        Iterator<ConfigAttribute> iter = config.iterator();

        while ( iter.hasNext() ) {
            ConfigAttribute attr = iter.next();

            if ( this.supports( attr ) ) {
                // Need to process the Collection for this invocation
                if ( returnedObject == null ) {
                    logger.debug( "Return object is null, skipping" );
                    return returnedObject;
                }

                Filterer<Object> filterer = null;

                if ( returnedObject instanceof Collection ) {
                    Collection<Object> collection = ( Collection<Object> ) returnedObject;
                    filterer = new CollectionFilterer<Object>( collection );
                } else if ( returnedObject.getClass().isArray() ) {
                    Object[] array = ( Object[] ) returnedObject;
                    filterer = new ArrayFilterer<Object>( array );
                } else {
                    throw new UnsupportedOperationException( "Must be a Collection" );
                }

                // Locate unauthorised Collection elements
                Iterator<Object> collectionIter = filterer.iterator();

                /*
                 * Collect up the securevalueobjects
                 */
                Collection<SecureValueObject> securablesToFilter = new HashSet<SecureValueObject>();
                while ( collectionIter.hasNext() ) {
                    Object domainObject = collectionIter.next();
                    if ( !SecureValueObject.class.isAssignableFrom( domainObject.getClass() ) ) {
                        continue;
                    }
                    securablesToFilter.add( ( SecureValueObject ) domainObject );
                }

                Map<SecureValueObject, Boolean> hasPerm = securityService.hasPermission( securablesToFilter,
                        this.requirePermission, authentication );

                for ( SecureValueObject s : hasPerm.keySet() ) {
                    if ( !hasPerm.get( s ) ) {
                        filterer.remove( s );
                    }
                }

                if ( ( ( Collection<SecureValueObject> ) filterer.getFilteredObject() ).isEmpty() ) {
                    return filterer.getFilteredObject();
                }

                // Following are only relevant if you are logged in.
                if ( !SecurityServiceImpl.isUserLoggedIn() ) {
                    return filterer.getFilteredObject();
                }

                StopWatch timer = new StopWatch();
                timer.start();

                Map<Securable, Acl> acls = securityService.getAcls( ( Collection<SecureValueObject> ) filterer
                        .getFilteredObject() );

                Map<Securable, Boolean> areOwnedByCurrentUser = securityService
                        .areOwnedByCurrentUser( ( Collection<SecureValueObject> ) filterer.getFilteredObject() );
                boolean userIsAdmin = SecurityServiceImpl.isUserAdmin();

                // Only need to check for write permissions if we can't already infer it.
                Map<SecureValueObject, Boolean> canWrite = new HashMap<SecureValueObject, Boolean>();
                if ( !userIsAdmin && !requirePermission.contains( BasePermission.WRITE ) ) {
                    List<Permission> writePermissions = new ArrayList<Permission>();
                    writePermissions.add( BasePermission.WRITE );
                    canWrite = securityService.hasPermission( securablesToFilter, this.requirePermission,
                            authentication );
                }

                for ( Securable s : acls.keySet() ) {

                    /*
                     * Populate optional fields in the ValueObject.
                     */

                    SecureValueObject svo = ( SecureValueObject ) s;

                    // this should be fast, but could be even faster.
                    Acl acl = acls.get( s );
                    svo.setIsPublic( !SecurityUtil.isPrivate( acl ) );
                    svo.setIsShared( SecurityUtil.isShared( acl ) );
                    svo.setUserOwned( areOwnedByCurrentUser.get( s ) );

                    if ( svo.getUserOwned() || userIsAdmin || requirePermission.contains( BasePermission.WRITE ) ) {
                        svo.setUserCanWrite( true );
                    } else {
                        svo.setUserCanWrite( canWrite.containsKey( s ) && canWrite.get( s ) );
                    }
                }

                if ( timer.getTime() > 100 ) {
                    logger.info( "Fill in security details on " + acls.keySet().size() + " value objects: "
                            + timer.getTime() + "ms" );
                }
                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }
}
