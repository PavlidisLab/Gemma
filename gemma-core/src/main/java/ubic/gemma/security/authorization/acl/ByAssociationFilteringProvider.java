/*
 * The Gemma project
 * 
 * Copyright (c) 2008-2010 University of British Columbia
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * Subclass this when you want to filter collections based not on the security of the object itself, but by an
 * associated object. For example, a collection of CompositeSequences is filtered based on security of the associated
 * ArrayDesign.
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class ByAssociationFilteringProvider<T extends Securable, A> extends AbstractAclProvider {

    public ByAssociationFilteringProvider( AclService aclService, String processConfigAttribute,
            List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    protected static final Log logger = LogFactory.getLog( ByAssociationFilteringProvider.class );

    /**
     * Given one of the input objects (which is not securable) return the associated securable.
     * 
     * @param targetDomainObject
     * @return
     */
    protected abstract T getAssociatedSecurable( Object targetDomainObject );

    public abstract String getProcessConfigAttribute();

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
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Return object is null, skipping" );
                    }

                    return null;
                }

                Filterer<A> filterer = null;

                boolean wasSingleton = false;
                if ( returnedObject instanceof Collection ) {
                    Collection collection = ( Collection<A> ) returnedObject;
                    filterer = new CollectionFilterer<A>( collection );
                } else if ( returnedObject.getClass().isArray() ) {
                    A[] array = ( A[] ) returnedObject;
                    filterer = new ArrayFilterer<A>( array );
                } else {
                    // shortcut, just put the object in a collection. (PP)
                    wasSingleton = true;
                    Collection<A> coll = new HashSet<A>();
                    coll.add( ( A ) returnedObject );
                    filterer = new CollectionFilterer<A>( coll );
                }

                // Locate unauthorised Collection elements
                Iterator<A> collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {

                    A targetDomainObject = collectionIter.next();
                    T domainObject = getAssociatedSecurable( targetDomainObject );

                    boolean hasPermission = false;

                    if ( domainObject == null ) {
                        hasPermission = true;
                    } else {
                        hasPermission = hasPermission( authentication, domainObject );
                    }

                    if ( !hasPermission ) {
                        filterer.remove( targetDomainObject );

                        if ( logger.isDebugEnabled() ) {
                            logger.debug( "Principal is NOT authorised for element: " + targetDomainObject );
                        }
                    }
                }
                if ( wasSingleton ) {
                    if ( ( ( Collection<A> ) filterer.getFilteredObject() ).size() == 1 ) {
                        return ( ( Collection<A> ) filterer.getFilteredObject() ).iterator().next();
                    }
                    return null;

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
    @Override
    public final boolean supports( ConfigAttribute attribute ) {
        if ( ( attribute.getAttribute() != null ) && attribute.getAttribute().equals( getProcessConfigAttribute() ) ) {
            return true;
        }
        return false;
    }

    /**
     * This base implementation supports any type of class, because it does not query the presented secure object.
     * Subclasses can provide a more specific implementation.
     * 
     * @param clazz the secure object
     * @return always <code>true</code>
     */
    @Override
    public boolean supports( Class<?> clazz ) {
        return true;
    }

}
