/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package gemma.gsec.acl.afterinvocation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;

/**
 * Overrides default behaviour by returning null, rather than throwing an access denied exception
 *
 * @author paul
 * @version $Id: AclEntryAfterInvocationQuietProvider.java,v 1.3 2013/09/14 16:56:03 paul Exp $
 */
public class AclEntryAfterInvocationProvider extends org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider {

    private static final Log log = LogFactory.getLog( AclEntryAfterInvocationProvider.class );

    private boolean quiet;

    public AclEntryAfterInvocationProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, requirePermission );
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
        Object returnedObject ) throws AccessDeniedException {
        try {
            return super.decide( authentication, object, config, returnedObject );
        } catch ( AccessDeniedException e ) {
            String processConfigAttributeQuiet = processConfigAttribute + "_QUIET";
            if ( quiet && config.stream().map( ConfigAttribute::getAttribute ).anyMatch( processConfigAttributeQuiet::equals ) ) {

                // This is expected when user is anonymous, etc.
                // log.warn( "Access denied to: " + object );
                if ( log.isDebugEnabled() ) log.debug( e + ": returning null" );
                return null;
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean supports( ConfigAttribute attribute ) {
        return super.supports( attribute )
            || ( quiet && ( processConfigAttribute + "_QUIET" ).equals( attribute.getAttribute() ) );
    }

    /**
     * If set to true, this provider will return null rather than throwing an {@link AccessDeniedException}.
     * <p>
     * To work, you must add the {@code _QUIET} suffix to the config attribute.
     */
    public void setQuiet( boolean quiet ) {
        this.quiet = quiet;
    }
}
