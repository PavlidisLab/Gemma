/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.security.afterInvocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.acl.AclEntry;
import org.springframework.security.acl.AclManager;
import org.springframework.security.acl.basic.AbstractBasicAclEntry;
import org.springframework.security.acl.basic.SimpleAclEntry;
import org.springframework.security.afterinvocation.AfterInvocationProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.Securable;

/**
 * Subclass this when you want to filter collections based not on the security of the object itself, but by an
 * associated object.
 * 
 * @author Paul
 * @version $Id$
 */
@SuppressWarnings("deprecation")
public abstract class ByAssociatedObjectFilter implements AfterInvocationProvider {
    protected static final Log logger = LogFactory.getLog( ByAssociatedObjectFilter.class );

    private AclManager aclManager;

    private int[] requirePermission = { SimpleAclEntry.READ }; // default.

    public void setAclManager( AclManager aclManager ) {
        this.aclManager = aclManager;
    }

    public AclManager getAclManager() {
        return aclManager;
    }

    protected abstract Securable getDomainObject( Object targetDomainObject );

    public abstract String getProcessConfigAttribute();

    public void setRequirePermission( int[] requirePermission ) {
        this.requirePermission = requirePermission;
    }

    public int[] getRequirePermission() {
        return requirePermission;
    }

    /**
     * Decides whether user has access to object based on owning object (for composition relationships).
     * 
     * @param authentication
     * @param object
     * @param config
     * @param returnedObject
     * @return Object
     * @throws AccessDeniedException
     */
    @SuppressWarnings("unused")
    public final Object decide( Authentication authentication, Object object, ConfigAttributeDefinition config,
            Object returnedObject ) throws AccessDeniedException {
        
        Collection configAttribs = config.getConfigAttributes();
        Iterator iter = configAttribs.iterator();

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

                Filterer filterer = null;

                boolean wasSingleton = false;
                if ( returnedObject instanceof Collection ) {
                    Collection collection = ( Collection ) returnedObject;
                    filterer = new CollectionFilterer( collection );
                } else if ( returnedObject.getClass().isArray() ) {
                    Object[] array = ( Object[] ) returnedObject;
                    filterer = new ArrayFilterer( array );
                } else {
                    // shortcut, just put the object in a collection. (PP)
                    wasSingleton = true;
                    Collection<Object> coll = new HashSet<Object>();
                    coll.add( returnedObject );
                    filterer = new CollectionFilterer( coll );
                }

                // Locate unauthorised Collection elements
                Iterator collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {

                    Object targetDomainObject = collectionIter.next();
                    Object domainObject = getDomainObject( targetDomainObject );

                    boolean hasPermission = false;

                    AclEntry[] acls = null;

                    if ( domainObject == null ) {
                        hasPermission = true;
                    } else {
                        // get acl for domainObject that has been granted to the user.
                        acls = aclManager.getAcls( domainObject, authentication );
                    }

                    if ( ( acls != null ) && ( acls.length != 0 ) ) {
                        for ( int i = 0; i < acls.length; i++ ) {
                            // Locate processable AclEntrys
                            if ( acls[i] instanceof AbstractBasicAclEntry ) {
                                AbstractBasicAclEntry processableAcl = ( AbstractBasicAclEntry ) acls[i];

                                // See if principal has any of the required permissions
                                for ( int y = 0; y < requirePermission.length; y++ ) {
                                    if ( processableAcl.isPermitted( requirePermission[y] ) ) {
                                        hasPermission = true;

                                        if ( logger.isDebugEnabled() ) {
                                            logger.debug( "Principal is authorised for element: " + targetDomainObject
                                                    + " due to ACL: " + processableAcl.toString() );
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ( !hasPermission ) {
                        filterer.remove( domainObject );

                        if ( logger.isDebugEnabled() ) {
                            logger.debug( "Principal is NOT authorised for element: " + targetDomainObject );
                        }
                    }
                }
                if ( wasSingleton ) {
                    if ( ( ( Collection ) filterer.getFilteredObject() ).size() == 1 ) {
                        return ( ( Collection ) filterer.getFilteredObject() ).iterator().next();
                    } else {
                        return null;
                    }

                }
                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }

    /**
     * Called by the AbstractSecurityInterceptor at startup time to determine of AfterInvocationManager can process the
     * ConfigAttribute.
     * 
     * @param attribute
     * @return boolean
     */
    public final boolean supports( ConfigAttribute attribute ) {
        if ( ( attribute.getAttribute() != null ) && attribute.getAttribute().equals( getProcessConfigAttribute() ) ) {
            return true;
        }
        return false;
    }

    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     * 
     * @param clazz the secure object
     * @return always <code>true</code>
     */
    @SuppressWarnings("unused")
    public boolean supports( Class clazz ) {
        return true;
    }

}
