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
import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.SecurityConfig;
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.runas.RunAsManagerImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.SecurableDao;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean name="securityService"
 * @spring.property name="basicAclExtendedDao" ref="basicAclExtendedDao"
 * @spring.property name="securableDao" ref="securableDao"
 */
public class SecurityService {
    private Log log = LogFactory.getLog( SecurityService.class );

    private BasicAclExtendedDao basicAclExtendedDao = null;
    private SecurableDao securableDao = null;

    private final int PUBLIC_MASK = 6;
    private final int PRIVATE_MASK = 0;

    /**
     * Changes the acl_permission of the object to either administrator/PRIVATE (mask=0), or read-write/PUBLIC (mask=6).
     * 
     * @param object
     * @param mask
     */
    public void makePrivate( Object object, int mask ) {
        log.debug( "Changing acl of object " + object + "." );

        if ( mask != PUBLIC_MASK && mask != PRIVATE_MASK ) {
            throw new RuntimeException( "Supported masks are 0 (PRIVATE) and 6 (PUBLIC)." );
        }

        SecurityContext securityCtx = SecurityContextHolder.getContext();
        Authentication authentication = securityCtx.getAuthentication();
        Object principal = authentication.getPrincipal();

        if ( object instanceof Securable ) {

            String recipient = checkWhoToRunAs( object, mask, authentication, principal );
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
     * Runs as the recipient in acl_permission if the principal does not match the recipient.
     * 
     * @param object
     * @param mask
     * @param authentication
     * @param principal
     */
    private String checkWhoToRunAs( Object object, int mask, Authentication authentication, Object principal ) {

        Securable securedObject = ( Securable ) object;
        /* id of target object */
        Long id = securedObject.getId();

        /* id of acl_object_identity */
        Long objectIdentityId = securableDao.getAclObjectIdentityId( object, id );
        String recipient = securableDao.getRecipient( objectIdentityId );

        if ( !recipient.equals( principal.toString() ) ) {
            RunAsManagerImpl runAsManager = new RunAsManagerImpl();
            runAsManager.setKey( recipient );
            ConfigAttributeDefinition attributeDefinition = new ConfigAttributeDefinition();
            attributeDefinition.addConfigAttribute( new SecurityConfig( "RUN_AS_" + recipient ) );
            runAsManager.buildRunAs( authentication, object, attributeDefinition );

        } else {
            recipient = principal.toString();
        }
        return recipient;

    }

    /**
     * @param aclDao the aclDao to set
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    /**
     * @param securableDao the securableDao to set
     */
    public void setSecurableDao( SecurableDao securableDao ) {
        this.securableDao = securableDao;
    }

}
