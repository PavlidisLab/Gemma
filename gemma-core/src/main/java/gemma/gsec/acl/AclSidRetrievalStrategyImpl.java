/*
 * The gemma-core project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gemma.gsec.acl;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Sid retrieval strategy that mirrors Spring Security's stock {@link
 * org.springframework.security.acls.domain.SidRetrievalStrategyImpl} but threads through an
 * injected {@link RoleHierarchy} (Spring's stock impl uses a NullRoleHierarchy by default).
 * <p>
 * Renovations Phase 2: this returns Spring Security's {@link PrincipalSid} / {@link
 * GrantedAuthoritySid} rather than gsec's AclPrincipalSid / AclGrantedAuthoritySid so the user's
 * sids equate to the ACE sids that {@code BasicLookupStrategy} builds when loading ACLs from the
 * canonical schema. Spring's Sid.equals does a type-strict {@code instanceof} check and would
 * otherwise reject gsec-typed user sids against Spring-typed ACE sids — making every
 * {@code AclEntryVoter} vote AccessDenied.
 *
 * @author Paul
 * @version $Id: AclSidRetrievalStrategyImpl.java,v 1.1 2013/09/14 16:56:00 paul Exp $
 * @see org.springframework.security.acls.domain.SidRetrievalStrategyImpl
 */
public class AclSidRetrievalStrategyImpl implements SidRetrievalStrategy {

    private final RoleHierarchy roleHierarchy;

    public AclSidRetrievalStrategyImpl( RoleHierarchy roleHierarchy ) {
        Assert.notNull( roleHierarchy, "RoleHierarchy must not be null" );
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    public List<Sid> getSids( Authentication authentication ) {
        Collection<? extends GrantedAuthority> authorities = roleHierarchy
            .getReachableGrantedAuthorities( authentication.getAuthorities() );
        List<Sid> sids = new ArrayList<>( authorities.size() + 1 );

        sids.add( new PrincipalSid( authentication ) );

        for ( GrantedAuthority authority : authorities ) {
            sids.add( new GrantedAuthoritySid( authority ) );
        }

        return sids;
    }

}
