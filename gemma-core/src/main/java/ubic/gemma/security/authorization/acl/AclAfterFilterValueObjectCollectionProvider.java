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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import ubic.gemma.model.common.auditAndSecurity.SecureValueObject;
import ubic.gemma.security.SecurityService;




/**
 * @author cmcdonald
 *
 */
public class AclAfterFilterValueObjectCollectionProvider extends AbstractAclProvider {
    
    protected static final Log logger = LogFactory.getLog(AclAfterFilterValueObjectCollectionProvider.class);

    public AclAfterFilterValueObjectCollectionProvider(AclService aclService, List<Permission> requirePermission) {
        super(aclService, "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ", requirePermission);
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

                    return null;
                }

                Filterer<Object> filterer = null;

                boolean wasSingleton = false;
                if ( returnedObject instanceof Collection ) {
                    Collection<Object> collection = ( Collection<Object> ) returnedObject;
                    filterer = new CollectionFilterer<Object>( collection );
                } else if ( returnedObject.getClass().isArray() ) {
                    Object[] array = ( Object[] ) returnedObject;
                    filterer = new ArrayFilterer<Object>( array );
                } else {
                    // shortcut, just put the object in a collection. (PP)
                    wasSingleton = true;
                    Collection<Object> coll = new HashSet<Object>();
                    coll.add( returnedObject );
                    filterer = new CollectionFilterer<Object>( coll );
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
                
                
                //you will only ever want to read securevalueobjects
                List<Permission> requiredPermissions = new ArrayList<Permission>();
                requiredPermissions.add( BasePermission.READ );

                for ( SecureValueObject s : securablesToFilter ) {

                    if ( !securityService.hasPermission( s, requiredPermissions ,authentication ) ) {
                        filterer.remove( s );
                    }
                }

                if ( wasSingleton ) {
                    if ( ( ( Collection<SecureValueObject> ) filterer.getFilteredObject() ).size() == 1 ) {
                        return ( ( Collection<SecureValueObject> ) filterer.getFilteredObject() ).iterator().next();
                    }
                    return null;

                }
                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }
}
