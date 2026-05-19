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

import ubic.gemma.core.security.acl.domain.AclService;
import org.hibernate.Session;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * After-invocation provider that filters a {@link Stream} of domain objects, retaining only those for which the
 * authenticated user holds one of the {@code requirePermission} grants. Opens its own Hibernate session (via
 * {@link AclService#openSession()}) so per-element ACL lookups don't contend with the calling service's session, and
 * closes the session when the returned stream is closed.
 * <p>
 * Phase 3 AfterInvocation Phase B port: verbatim port of
 * {@code ubic.gemma.core.security.acl.afterinvocation.AclEntryAfterInvocationStreamFilteringProvider} into gemma-core so the
 * AfterInvocation provider chain no longer depends on the gsec class. Note this provider requires the gsec
 * {@link AclService} subtype (not just the stock Spring Security {@code AclService}) because it needs the
 * {@code openSession()} / {@code readAclById(oid, session)} extension methods — that gsec class is part of the ACL
 * persistence layer and is out of scope for this AfterInvocation migration.
 *
 * @author poirigui
 */
public class AclEntryAfterInvocationStreamFilteringProvider extends AbstractAclProvider {

    public static final String ATTRIBUTE = "AFTER_ACL_STREAM_READ";

    private final AclService aclService;

    public AclEntryAfterInvocationStreamFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, ATTRIBUTE, requirePermission );
        this.aclService = aclService;
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config, Object returnedObject ) throws AccessDeniedException {
        for ( ConfigAttribute configAttribute : config ) {
            if ( !supports( configAttribute ) ) {
                continue;
            }
            if ( returnedObject instanceof Stream ) {
                Session session = aclService.openSession();
                List<Sid> sids = sidRetrievalStrategy.getSids( authentication );
                return ( ( Stream<?> ) returnedObject )
                        .filter( getProcessDomainObjectClass()::isInstance )
                        .filter( s -> hasPermission( s, sids, session ) )
                        .onClose( session::close );
            } else {
                throw new AuthorizationServiceException( "Expected a return type of Stream, but got " + returnedObject + "." );
            }
        }
        return returnedObject;
    }

    private boolean hasPermission( Object domainObject, List<Sid> sids, Session session ) {
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );
        try {
            // Lookup only ACLs for SIDs we're interested in
            Acl acl = aclService.readAclById( objectIdentity, session );
            return acl.isGranted( requirePermission, sids, false );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }
}
