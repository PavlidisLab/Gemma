/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.BaseAclAdvice;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.GroupAuthority;
import gemma.gsec.model.Securable;
import gemma.gsec.model.User;
import gemma.gsec.model.UserGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Gemma-specific extension points for {@link BaseAclAdvice}: identifies User / UserGroup,
 * supplies their granted authority, marks the entity types that should keep private ACLs on
 * admin-driven creation, and runs the DEA → ExpressionAnalysisResultSet parent-ACL
 * special case. Triggered from {@link gemma.gsec.acl.AclEventListener} via the
 * BaseAclAdvice protected hooks; no longer an AOP advice (Renovations Phase 3 dropped the
 * {@code @AfterReturning} wiring).
 *
 * @author Paul
 */
@Component
public class AclAdvice extends BaseAclAdvice {

    private static final Log log = LogFactory.getLog( AclAdvice.class );

    @Autowired
    public AclAdvice( AclService aclService, SessionFactory sessionFactory, ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy ) {
        super( aclService, sessionFactory, objectIdentityRetrievalStrategy );
    }

    @Override
    protected boolean canSkipAclCheck( Object object ) {
        return AuditTrail.class.isAssignableFrom( object.getClass() ) || CurationDetails.class
                .isAssignableFrom( object.getClass() );
    }

    @Override
    protected void createOrUpdateAclSpecialCases( MutableAcl acl, @Nullable Acl parentAcl, Sid sid, Securable object ) {
        // make sure that result sets have the ACLs created and setup to inherit those from the DEA
        if ( object instanceof DifferentialExpressionAnalysis ) {
            for ( ExpressionAnalysisResultSet resultSet : ( ( DifferentialExpressionAnalysis ) object ).getResultSets() ) {
                // Renovations Phase 3: under the AclEventListener path this special case fires
                // during DEA's PostInsertEvent — DEA's cascaded ResultSets haven't been inserted
                // yet so their ids are null. Skip them here; each ResultSet's own
                // PostInsertEvent will trigger ACL creation, and the listener's
                // locateParentAcl(SecuredChild) walks ResultSet.getSecurityOwner() back to the
                // now-persisted DEA to set the parent inheritance correctly. The pre-renovation
                // AOP advice path is unaffected: it fires @AfterReturning after the tx-scoped
                // flush has assigned ids, so ResultSets reach this code with non-null ids.
                if ( resultSet.getId() == null ) {
                    continue;
                }
                ObjectIdentity rsOi = makeObjectIdentity( resultSet );
                MutableAcl rsAcl;
                try {
                    rsAcl = ( MutableAcl ) getAclService().readAclById( rsOi );
                } catch ( NotFoundException e ) {
                    log.warn( "No ACL identity found for " + resultSet + ", creating a new one." );
                    rsAcl = getAclService().createAcl( rsOi );
                }
                if ( rsAcl.getParentAcl() == null ) {
                    log.warn( "ACL for " + resultSet + " does not have a parent populated, setting it to " + acl + "." );
                    rsAcl.setParent( acl );
                    rsAcl.setEntriesInheriting( true );
                    getAclService().updateAcl( rsAcl );
                }
            }
        }
    }

    @Override
    protected GrantedAuthority getUserGroupGrantedAuthority( Securable object ) {
        Collection<? extends GroupAuthority> authorities = ( ( UserGroup ) object ).getAuthorities();
        assert authorities.size() == 1;
        return new SimpleGrantedAuthority( authorities.iterator().next().getAuthority() );
    }

    @Override
    protected String getUserName( Securable user ) {
        return ( ( User ) user ).getUserName();
    }

    @Override
    protected boolean objectIsUser( Securable object ) {
        return User.class.isAssignableFrom( object.getClass() );
    }

    @Override
    protected boolean objectIsUserGroup( Securable object ) {
        return UserGroup.class.isAssignableFrom( object.getClass() );
    }

    @Override
    protected boolean specialCaseToKeepPrivateOnCreation( Securable object ) {
        return super.specialCaseToKeepPrivateOnCreation( object )
                || object instanceof UserGroup
                || object instanceof User
                || object instanceof Investigation;
    }
}
