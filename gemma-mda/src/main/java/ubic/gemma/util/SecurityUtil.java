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
package ubic.gemma.util;

import org.acegisecurity.userdetails.UserDetails;
import org.hibernate.proxy.HibernateProxy;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserImpl;

/**
 * @author keshav
 * @version $Id$
 */
public class SecurityUtil {
    /**
     * Returns the Implementation object from the HibernateProxy. If target is not an instanceof HibernateProxy, target
     * is returned.
     * 
     * @param target The proxy
     * @return Object The implementation
     */
    public static Object getImplementationFromProxy( Object target ) {
        if ( target instanceof HibernateProxy ) {
            HibernateProxy proxy = ( HibernateProxy ) target;
            return proxy.getHibernateLazyInitializer().getImplementation();
        }

        return target;
    }

    /**
     * @param userDetails
     * @return {@link User}
     */
    public static User getUserFromUserDetails( UserDetails userDetails ) {
        User user = new UserImpl();
        user.setName( userDetails.getUsername() );
        user.setPassword( userDetails.getPassword() );

        return user;
    }
}
