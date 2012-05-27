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
 * Filter a one-to-one map where the keys are NON-SECURABLE and the values ARE securable (or at least, can be). The
 * values can be a mixture of securable or non-securable. If you are using a map where both they keys and values are
 * securable, use {@link AclAfterInvocationMapFilteringProvider}
 * 
 * @author paul
 * @version $Id$
 * @see AclAfterInvocationMapFilteringProvider
 */
public class AclAfterInvocationMapValueFilteringProvider extends AbstractAclProvider {

    public AclAfterInvocationMapValueFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_MAP_VALUES_READ", requirePermission );
    }

    protected static final Log logger = LogFactory.getLog( AclAfterInvocationMapFilteringProvider.class );

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.security.access.AfterInvocationProvider#decide(org.springframework.security.core.Authentication
     * , java.lang.Object, java.util.Collection, java.lang.Object)
     */
    @Override
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

                if ( returnedObject instanceof Map ) {
                    Map<? extends Object, Object> map = ( Map<? extends Object, Object> ) returnedObject;
                    filterer = new MapValueFilterer( map );
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
                    } else if ( !Securable.class.isAssignableFrom( domainObject.getClass() ) ) {
                        hasPermission = true;
                    } else {
                        hasPermission = hasPermission( authentication, domainObject );
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
