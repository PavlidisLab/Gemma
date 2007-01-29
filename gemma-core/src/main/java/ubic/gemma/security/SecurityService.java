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
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Securable;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean name="securityService"
 * @spring.property name="basicAclExtendedDao" ref="basicAclExtendedDao"
 */
public class SecurityService {
    private Log log = LogFactory.getLog( SecurityService.class );

    private BasicAclExtendedDao basicAclExtendedDao = null;

    private final int PUBLIC_MASK = 6;
    private final int PRIVATE_MASK = 1;

    /**
     * Changes the acl_permission of the object to either administrator/PUBLIC (mask=1), or read-write/PRIVATE (mask=6).
     * 
     * @param object
     * @param mask
     */
    public void makePrivate( Object object, int mask ) {
        log.debug( "Changing acl of object " + object + "." );

        if ( mask != PUBLIC_MASK && mask != PRIVATE_MASK ) {
            throw new RuntimeException( "Supported masks are 1 (PUBLIC) and 6 (PRIVATE)." );
        }

        SecurityContext securityCtx = SecurityContextHolder.getContext();
        Authentication authentication = securityCtx.getAuthentication();
        Object recipient = authentication.getPrincipal();

        if ( object instanceof Securable ) {

            try {
                basicAclExtendedDao.changeMask( new NamedEntityObjectIdentity( object ), recipient, mask );
            } catch ( Exception e ) {
                throw new RuntimeException( "Problems changing mask of " + object, e );
            }
        }

        else {
            throw new RuntimeException( "Object not Securable.  Cannot change permissions for object of type " + object
                    + "." );
        }

    }

    /**
     * @param aclDao the aclDao to set
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

}
