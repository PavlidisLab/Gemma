/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * Like the AclEntryAfterInvocationCollectionFilteringProvider, but filters on the keys AND values of a Map, where the
 * keys are Securable and the values MAY be Securable. If your keys are non-securable, use
 * {@link AclAfterInvocationMapValueFilteringProvider}
 * 
 * @see org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider
 * @author paul
 * @version $Id$
 * @see AclAfterInvocationMapValueFilteringProvider.java
 */
public class AclAfterInvocationMapFilteringProvider extends AbstractAclProvider {

    public AclAfterInvocationMapFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_MAP_READ", requirePermission );
    }

    protected static final Log logger = LogFactory.getLog( AclAfterInvocationMapFilteringProvider.class );

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.security.access.AfterInvocationProvider#decide(org.springframework.security.core.Authentication
     * , java.lang.Object, java.util.Collection, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        Iterator<?> iter = config.iterator();

        while ( iter.hasNext() ) {
            ConfigAttribute attr = ( ConfigAttribute ) iter.next();

            if ( this.supports( attr ) ) {
                // Need to process the Collection for this invocation
                if ( returnedObject == null ) {
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Return object is null, skipping" );
                    }

                    return null;
                }

                Filterer<Object> filterer = null;
                Map<? extends Object, Object> map;

                if ( returnedObject instanceof Map ) {
                    map = ( Map<? extends Object, Object> ) returnedObject;
                    filterer = new MapFilterer( map );
                } else {
                    throw new AuthorizationServiceException( "A Map was required as the "
                            + "returnedObject, but the returnedObject was: " + returnedObject );
                }

                // Locate unauthorised Collection elements
                Iterator<Object> collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {
                    Object domainObject = collectionIter.next();
                    boolean hasPermission = false;
                    if ( domainObject == null ) {
                        hasPermission = true;
                        continue;
                    }

                    /*
                     * If the key is not a securable, it's okay; if it is we need explicit permission
                     */
                    hasPermission = Securable.class.isAssignableFrom( domainObject.getClass() )
                            || hasPermission( authentication, domainObject );

                    /*
                     * Check the VALUE as well.
                     */
                    Object value = map.get( domainObject );
                    if ( value != null && Securable.class.isAssignableFrom( value.getClass() ) ) {
                        hasPermission = hasPermission( authentication, value ) && hasPermission;
                    }

                    if ( !hasPermission ) {
                        filterer.remove( domainObject );

                        if ( logger.isDebugEnabled() ) {
                            logger.debug( "Principal is NOT authorised for element: " + domainObject );
                        }
                    }
                }

                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }
}
