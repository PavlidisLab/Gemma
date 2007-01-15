/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.acl.AclEntry;
import org.acegisecurity.acl.AclManager;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * @author keshav
 * @spring.bean name="securityService"
 * @spring.property name="aclManager" ref="aclManager"
 */
public class SecurityService {
    private Log log = LogFactory.getLog( SecurityService.class );

    private AclManager aclManager = null;

    /**
     * @param object
     */
    public void makePrivate( Object object ) {

        if ( object instanceof ArrayDesign ) {
            ArrayDesign arrayDesign = ( ArrayDesign ) object;

            SecurityContext securityCtx = SecurityContextHolder.getContext();

            Authentication authentication = securityCtx.getAuthentication();
            GrantedAuthority[] grantedAuthorities = authentication.getAuthorities();
            for ( GrantedAuthority authority : grantedAuthorities ) {
                log.debug( "Authority: " + authority.getAuthority() );
            }

            AclEntry[] acls = aclManager.getAcls( arrayDesign );
            for ( AclEntry acl : acls ) {
                log.debug( "acl entry: " + acl );
                // I want to remove acl entries that are not administrator
            }

            // I need a setter to set the acls. AclManager does not have one.
        }

    }

    /**
     * @param aclManager the aclManager to set
     */
    public void setAclManager( AclManager aclManager ) {
        this.aclManager = aclManager;
    }

}
