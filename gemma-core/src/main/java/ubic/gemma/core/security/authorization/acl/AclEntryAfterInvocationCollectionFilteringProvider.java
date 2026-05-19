/*
 * The gsec project (ported into gemma-core for Phase 3 AfterInvocation modernization)
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
 */
package ubic.gemma.core.security.authorization.acl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Overrides the functionality of the spring-provided
 * {@link org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider}
 * to be more efficient with large collections by using {@link AclService#readAclsById(List, List)}
 * to bulk-fetch ACLs for every element of the returned collection in a single query.
 * <p>
 * Phase 3 AfterInvocation Phase B port: this class is a verbatim port of
 * {@code gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider}
 * into gemma-core so the AfterInvocation provider chain has no remaining runtime dependency
 * on the gsec class. Behaviorally identical; the Gemma subclasses
 * ({@link AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider},
 * {@link AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider},
 * {@link AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider},
 * etc.) extend this rather than the gsec original.
 *
 * @author Paul
 */
public class AclEntryAfterInvocationCollectionFilteringProvider extends org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider {

    private static final Log log = LogFactory.getLog( AclEntryAfterInvocationCollectionFilteringProvider.class );

    private static class DomainObjectWithPermission {
        private final Object domainObject;
        private final boolean permission;

        private DomainObjectWithPermission( Object domainObject, boolean permission ) {
            this.domainObject = domainObject;
            this.permission = permission;
        }
    }

    private final ThreadLocal<Iterator<DomainObjectWithPermission>> domainObjectsWithPermission = new ThreadLocal<>();

    @SuppressWarnings("unused")
    public AclEntryAfterInvocationCollectionFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, requirePermission );
    }

    protected AclEntryAfterInvocationCollectionFilteringProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, requirePermission );
        setProcessConfigAttribute( processConfigAttribute );
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        for ( ConfigAttribute attr : config ) {
            if ( !this.supports( attr ) ) {
                continue;
            }

            List<Object> domainObjects;
            if ( returnedObject instanceof Collection ) {
                domainObjects = new ArrayList<>( ( Collection<?> ) returnedObject );
            } else if ( returnedObject.getClass().isArray() ) {
                domainObjects = Arrays.asList( ( Object[] ) returnedObject );
            } else {
                throw new AuthorizationServiceException( "A Collection or an array (or null) was required as the "
                        + "returnedObject, but the returnedObject was: " + returnedObject );
            }

            // skip unsupported domain objects
            List<Object> retainedDomainObjects = domainObjects.stream()
                    .filter( getProcessDomainObjectClass()::isInstance )
                    .collect( Collectors.toList() );

            // compute the permissions in bulk
            boolean[] permissions = hasPermission( authentication, retainedDomainObjects );

            List<DomainObjectWithPermission> dowp = new ArrayList<>( retainedDomainObjects.size() );
            for ( int i = 0; i < permissions.length; i++ ) {
                dowp.add( new DomainObjectWithPermission( retainedDomainObjects.get( i ), permissions[i] ) );
            }
            domainObjectsWithPermission.set( dowp.iterator() );

            try {
                return super.decide( authentication, object, config, returnedObject );
            } finally {
                domainObjectsWithPermission.remove();
            }
        }

        return returnedObject;
    }

    /**
     * @deprecated use {@link #hasPermission(Authentication, List)} instead to benefit from bulk-loading of ACLs
     */
    @Override
    @Deprecated
    protected final boolean hasPermission( Authentication authentication, Object domainObject ) {
        if ( domainObjectsWithPermission.get() != null ) {
            DomainObjectWithPermission dowp = domainObjectsWithPermission.get().next();
            if ( domainObject == dowp.domainObject ) {
                return dowp.permission;
            } else {
                throw new IllegalStateException( String.format( "Unexpected domain object %s when processing ACLs in bulk; it is possible hasPermission() was invoked by a subclass?",
                        domainObject ) );
            }
        } else {
            return super.hasPermission( authentication, domainObject );
        }
    }

    /**
     * Bulk-processing version of {@link #hasPermission(Authentication, Object)}.
     */
    protected boolean[] hasPermission( Authentication authentication, List<Object> domainObjects ) {
        boolean[] perms = new boolean[domainObjects.size()];
        if ( domainObjects.isEmpty() ) {
            return perms;
        }
        List<Sid> sids = this.sidRetrievalStrategy.getSids( authentication );
        List<ObjectIdentity> ois = getObjectIdentities( domainObjects );
        Map<ObjectIdentity, Acl> aclsById;
        try {
            aclsById = aclService.readAclsById( ois, sids );
        } catch ( NotFoundException e ) {
            aclsById = Collections.emptyMap();
        }
        int i = 0;
        for ( ObjectIdentity oi : ois ) {
            Acl acl = aclsById.get( oi );
            boolean granted;
            if ( acl != null ) {
                try {
                    granted = hasPermission( acl, sids );
                } catch ( NotFoundException nfe ) {
                    // No ACE matched any required permission for any of the user's sids. Treat as
                    // "no permission" rather than propagating — the per-row filter should drop the
                    // domain object, not abort the whole filter pass.
                    log.trace( String.format( "No matching ACE for %s; filtering out.", oi ) );
                    granted = false;
                }
            } else {
                log.trace( String.format( "No ACL was found for %s.", oi ) );
                granted = false;
            }
            perms[i++] = granted;
        }
        return perms;
    }

    protected boolean hasPermission( Acl acl, List<Sid> sids ) {
        return acl.isGranted( requirePermission, sids, false );
    }

    protected List<ObjectIdentity> getObjectIdentities( List<Object> domainObjects ) {
        List<ObjectIdentity> result = new ArrayList<>( domainObjects.size() );
        for ( Object s : domainObjects ) {
            result.add( objectIdentityRetrievalStrategy.getObjectIdentity( s ) );
        }
        return result;
    }
}
