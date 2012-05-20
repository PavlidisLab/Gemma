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

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.Contact
 */
@Repository
public class ContactDaoImpl extends ubic.gemma.model.common.auditAndSecurity.ContactDaoBase {

    @Autowired
    public ContactDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.ContactDaoBase#find(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public Contact find( Contact contact ) {

        BusinessKey.checkKey( contact );
        Criteria queryObject = super.getSession().createCriteria( Contact.class );

        BusinessKey.addRestrictions( queryObject, contact );

        java.util.List<?> results = queryObject.list();
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

    }

    /*
     * (non-Javadoc)
     * 
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

    public Collection<Investigation> getInvestigations( Contact contact ) {
        /*
         * If there are other types of investigations they will have to be added to the results.
         */
        return this.getHibernateTemplate().findByNamedParam(
                "select e from ExpressionExperimentImpl e join e.investigators i where i=:c ", "c", contact );
    }

    @Override
    public Collection<Contact> findByName( String name ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from ContactImpl c where c.name like :d", "d", name + "%" );
    }
}