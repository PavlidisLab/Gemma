/*
 * The Gemma project (ported into gemma-core for Phase 3 AfterInvocation modernization)
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
 */
package ubic.gemma.core.security.authorization.acl;

import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

/**
 * Subclass this when filtering a collection based not on the security of the element itself but on the security of an
 * associated parent domain object. For example, a collection of {@code CompositeSequence}s is filtered by the ACL on
 * the associated {@code ArrayDesign}; a collection of {@code DataVector}s is filtered by the ACL on the associated
 * {@code ExpressionExperiment}.
 * <p>
 * Phase 3 AfterInvocation Phase B port: verbatim port of
 * {@code ubic.gemma.core.security.acl.afterinvocation.AclEntryAfterInvocationByAssociationCollectionFilteringProvider} into gemma-core
 * so the AfterInvocation provider chain has no remaining runtime dependency on the gsec class hierarchy. Behaviorally
 * identical to the gsec original.
 *
 * @author Paul
 */
@SuppressWarnings("unused")
public abstract class AclEntryAfterInvocationByAssociationCollectionFilteringProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    public AclEntryAfterInvocationByAssociationCollectionFilteringProvider( AclService aclService, String processConfigAttribute,
            List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    /**
     * Save time by getting the associated (parent) domain objects. Often there is just one (or a small number)
     * compared to the large number of target domain objects, so {@code readAclsById} on the parent collection bulk-
     * loads few ACLs.
     * <p>
     * Problem: I wanted to use a {@code Set} so I'd check permissions for the minimum number of objects. However we're
     * not in a transaction here, so the {@code Securable}s are often Hibernate proxies and can't be reliably hashed.
     *
     * @return an array of booleans in same order as the filterer's iterator containing {@code true} if the permission
     * is granted on the parent object, otherwise {@code false}
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
     * Obtain the associated (parent) domain object for which ACLs should be evaluated.
     */
    protected abstract Object getActualDomainObject( Object targetDomainObject );
}
