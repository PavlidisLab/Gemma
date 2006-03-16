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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.Contact
 */
public class ContactDaoImpl extends ubic.gemma.model.common.auditAndSecurity.ContactDaoBase {

    private static Log log = LogFactory.getLog( ContactDaoImpl.class.getName() );

    @Override
    public Contact find( Contact contact ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Contact.class );

            if ( contact.getName() != null ) queryObject.add( Restrictions.eq( "name", contact.getName() ) );

            if ( contact.getAddress() != null ) queryObject.add( Restrictions.eq( "address", contact.getAddress() ) );

            if ( contact.getEmail() != null ) queryObject.add( Restrictions.eq( "email", contact.getEmail() ) );

            if ( contact.getPhone() != null ) queryObject.add( Restrictions.eq( "phone", contact.getPhone() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.auditAndSecurity.Contact.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( ubic.gemma.model.common.auditAndSecurity.Contact ) results.iterator().next();
                }
            }
            return ( Contact ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Contact findOrCreate( Contact contact ) {
        if ( contact == null
                || ( contact.getName() == null && contact.getAddress() == null && contact.getEmail() == null && contact
                        .getPhone() == null ) ) {
            throw new IllegalArgumentException( "User must have at least some information filled in!" );
        }
        Contact newContact = find( contact );
        if ( newContact != null ) {
            BeanPropertyCompleter.complete( newContact, contact );
            return newContact;
        }
        return ( Contact ) create( contact );
    }
}