/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.Person
 */
@Repository
public class PersonDaoImpl extends ubic.gemma.model.common.auditAndSecurity.PersonDaoBase {

    @Autowired
    public PersonDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.PersonDao#find(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public Person find( Person person ) {
        try {
            Criteria queryObject = super.getSession().createCriteria( Person.class );

            BusinessKey.addRestrictions( queryObject, person );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.auditAndSecurity.Person.class.getName()
                                    + "' was found when executing query - " + person );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Person ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.PersonDao#findOrCreate(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public Person findOrCreate( Person person ) {
        if ( person == null
                || ( person.getLastName() == null && person.getAddress() == null && person.getEmail() == null
                        && person.getPhone() == null && person.getName() == null ) ) {
            throw new IllegalArgumentException( "Person did not have sufficient information for business key." );
        }
        Person newPerson = find( person );
        if ( newPerson != null ) {
            return newPerson;
        }
        return create( person );
    }

}