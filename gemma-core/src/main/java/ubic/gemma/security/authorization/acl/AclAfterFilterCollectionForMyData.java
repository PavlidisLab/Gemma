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
package ubic.gemma.security.authorization.acl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.security.SecurityService;

/**
 * Filter out public {@link Securables}s, leaving only ones that the user specifically been associated with. This is
 * used for the "my data" list.
 * 
 * @author keshav
 * @version $Id$
 * @see AfterInvocationProvider
 */
public class AclAfterFilterCollectionForMyData extends AbstractAclProvider {

    public AclAfterFilterCollectionForMyData( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_FILTER_MY_DATA", requirePermission );
    }

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private SecurityService securityService;

    /*
     * (non-Javadoc)
     * @seeorg.springframework.security.afterinvocation.AfterInvocationProvider#decide(org.springframework.security.
     * Authentication, java.lang.Object, org.springframework.security.ConfigAttributeDefinition, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public final Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        Iterator<ConfigAttribute> iter = config.iterator();

        while ( iter.hasNext() ) {
            ConfigAttribute attr = iter.next();

            if ( this.supports( attr ) ) {
                // Need to process the Collection for this invocation
                if ( returnedObject == null ) {
                    log.debug( "Return object is null, skipping" );

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
                    Collection coll = new HashSet();
                    coll.add( returnedObject );
                    filterer = new CollectionFilterer<Securable>( coll );
                }

                // Locate unauthorised Collection elements
                Iterator collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {

                    Object domainObject = collectionIter.next();

                    if ( !Securable.class.isAssignableFrom( domainObject.getClass() ) ) {
                        continue;
                    }

                    ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );

                    // Obtain the SIDs applicable to the principal. We're going to filter out anybody elses' anyway.
                    List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

                    Acl acl = aclService.readAclById( objectIdentity, sids );

                    if ( acl == null ) {
                        filterer.remove( domainObject );
                    } else

                    /*
                     * User has to have permission - this removes non-public data that doesn't belong to them
                     */
                    if ( !this.hasPermission( authentication, domainObject ) ) {
                        filterer.remove( domainObject );
                    }

                    /*
                     * Note: we might want to look at the ACL owner. That way they can view data that is public, but
                     * which they have ownership of.
                     */
                    else {
                        if ( securityService.isPublic( ( Securable ) domainObject ) ) {
                            filterer.remove( domainObject );
                        }
                    }

                }

                if ( wasSingleton ) {
                    if ( ( ( Collection ) filterer.getFilteredObject() ).size() == 1 ) {
                        return ( ( Collection ) filterer.getFilteredObject() ).iterator().next();
                    }
                    return null;

                }
                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }

}
