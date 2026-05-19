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
package gemma.gsec.acl.afterinvocation;

import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

/**
 * Subclass this when you want to filter collections based not on the security of the object itself, but by an
 * associated object. For example, a collection of CompositeSequences is filtered based on security of the associated
 * ArrayDesign.
 *
 * @author Paul
 * @version $Id: ByAssociationFilteringProvider.java,v 1.5 2013/09/14 16:56:01 paul Exp $
 */
@SuppressWarnings("unused")
public abstract class AclEntryAfterInvocationByAssociationCollectionFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    public AclEntryAfterInvocationByAssociationCollectionFilteringProvider( AclService aclService, String processConfigAttribute,
        List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    /**
     * Save time by getting the associated (parent) domain objects. Often there is just one; or a small number compared
     * to the large number of targetdomainobjects.
     * <p>
     * Problem: I wanted to use a Set, so I would check permissions for the minimum number of objects. However, we're not
     * in a transaction here, so the Securables are often proxies. So we can't hash them.
     *
     * @return an array of booleans in same order as the filterer's iterator containing true if the permission is
     * granted, otherwise false
     */
    @Override
    protected boolean[] hasPermission( Authentication authentication, List<Object> domainObjects ) {
        List<Object> actualDomainObjects = new ArrayList<>( domainObjects.size() );
        for ( Object domainObject : domainObjects ) {
            actualDomainObjects.add( getActualDomainObject( domainObject ) );
        }
        return super.hasPermission( authentication, actualDomainObjects );
    }

    /**
     * Obtain the associated domain object for which ACLs should be evaluated.
     */
    protected abstract Object getActualDomainObject( Object targetDomainObject );
}
