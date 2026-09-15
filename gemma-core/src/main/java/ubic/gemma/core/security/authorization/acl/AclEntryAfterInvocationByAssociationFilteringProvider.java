/*
 * The Gemma project (ported into gemma-core for Phase 3 AfterInvocation modernization)
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

import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Single-object after-invocation provider that evaluates ACLs on an associated (parent) domain object rather than on
 * the returned object itself. For example, the ACL on a returned {@code CompositeSequence} is evaluated by checking
 * the ACL on its associated {@code ArrayDesign}.
 * <p>
 * Phase 3 AfterInvocation Phase B port: verbatim port of
 * {@code ubic.gemma.core.security.acl.afterinvocation.AclEntryAfterInvocationByAssociationFilteringProvider} into gemma-core so the
 * AfterInvocation provider chain has no remaining runtime dependency on the gsec class hierarchy. Behaviorally
 * identical to the gsec original.
 */
@SuppressWarnings("unused")
public abstract class AclEntryAfterInvocationByAssociationFilteringProvider extends AclEntryAfterInvocationProvider {

    public AclEntryAfterInvocationByAssociationFilteringProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected boolean hasPermission( Authentication authentication, Object domainObject ) {
        return super.hasPermission( authentication, getActualDomainObject( domainObject ) );
    }

    /**
     * Obtain the associated (parent) domain object for which ACLs should be evaluated.
     */
    protected abstract Object getActualDomainObject( Object targetDomainObject );
}
