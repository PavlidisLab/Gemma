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

import org.hibernate.proxy.HibernateProxy;

/**
 * @author keshav
 * @version $Id$
 */
public class SecurityUtil {
    /**
     * Returns the Implementation object from the HibernateProxy. If target is not an instanceof HibernateProxy, target
     * is returned.
     * 
     * @param target
     * @return Object
     */
    public static Object getImplementationFromProxy( Object target ) {
        // TODO move method in a utility as it is accesseded by daos (SeurableDaoImpl)
        if ( target instanceof HibernateProxy ) {
            HibernateProxy proxy = ( HibernateProxy ) target;
            return proxy.getHibernateLazyInitializer().getImplementation();
        }

        return target;
    }
}
