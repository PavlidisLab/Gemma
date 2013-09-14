/*
 * The Gemma project
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
package ubic.gemma.security.authorization.acl;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;

import ubic.gemma.model.common.auditAndSecurity.SecuredChild;

/**
 * Specialization to allow handling of SecuredChild.
 * 
 * @author Paul
 * @version $Id$
 */
public class AclEntryVoter extends org.springframework.security.acls.AclEntryVoter {

    public AclEntryVoter( AclService aclService, String processConfigAttribute, Permission[] requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
        this.setObjectIdentityRetrievalStrategy( new ValueObjectAwareIdentityRetrievalStrategyImpl() );
        this.setSidRetrievalStrategy( new AclSidRetrievalStrategyImpl() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.access.vote.AbstractAclVoter#getDomainObjectInstance(java.lang.Object)
     */
    @Override
    protected Object getDomainObjectInstance( Object secureObject ) {
        Object[] args;
        Class<?>[] params;

        if ( secureObject instanceof MethodInvocation ) {
            MethodInvocation invocation = ( MethodInvocation ) secureObject;
            params = invocation.getMethod().getParameterTypes();
            args = invocation.getArguments();
        } else {
            JoinPoint jp = ( JoinPoint ) secureObject;
            params = ( ( CodeSignature ) jp.getStaticPart().getSignature() ).getParameterTypes();
            args = jp.getArgs();
        }

        for ( int i = 0; i < params.length; i++ ) {
            if ( getProcessDomainObjectClass().isAssignableFrom( params[i] ) ) {
                return args[i];
            }
        }

        // Start special case!
        for ( int i = 0; i < params.length; i++ ) {
            if ( SecuredChild.class.isAssignableFrom( params[i] ) ) {
                return ( ( SecuredChild ) args[i] ).getSecurityOwner();
            }
        }

        // voter will abstain.
        return null;

    }

}
