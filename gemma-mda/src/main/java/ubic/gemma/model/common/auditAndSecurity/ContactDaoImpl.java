/*
 * The Gemma project.
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
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Criteria;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.Contact
 */
public class ContactDaoImpl extends ubic.gemma.model.common.auditAndSecurity.ContactDaoBase {

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.ContactDaoBase#find(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @SuppressWarnings("unchecked")
    public Contact find( Contact contact ) {
        try {

            BusinessKey.checkKey( contact );
            Criteria queryObject = super.getSession( false ).createCriteria( Contact.class );

            BusinessKey.addRestrictions( queryObject, contact );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.auditAndSecurity.Contact.class.getName()
                                    + "' was found when executing query; query was " + contact + ", query object was "
                                    + queryObject );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Contact ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.ContactDaoBase#findOrCreate(ubic.gemma.model.common.auditAndSecurity
     * .Contact)
     */
    public Contact findOrCreate( Contact contact ) {

        Contact existingContact = find( contact );
        if ( existingContact != null ) {
            return existingContact;
        }
        return create( contact );
    }
}