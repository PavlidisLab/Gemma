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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.List;

/**
 * @author pavlidis
 * @see Contact
 */
@Repository
public class ContactDaoImpl extends AbstractDao<Contact> implements ContactDao {

    @Autowired
    public ContactDaoImpl( SessionFactory sessionFactory ) {
        super( Contact.class, sessionFactory );
    }

    @Override
    public Contact find( Contact contact ) {
        BusinessKey.checkKey( contact );
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Contact.class );
        BusinessKey.addRestrictions( queryObject, contact );
        List results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + Contact.class.getName()
                                + "' was found when executing query; query was " + contact + ", query object was "
                                + queryObject );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( Contact ) result;

    }
}