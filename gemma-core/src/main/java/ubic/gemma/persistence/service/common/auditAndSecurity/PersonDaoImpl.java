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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.Person</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.auditAndSecurity.Person
 * @deprecated as Person is deprecated
 */
@Repository
@Deprecated
public class PersonDaoImpl extends AbstractDao<Person> implements PersonDao {

    @Autowired
    public PersonDaoImpl( SessionFactory sessionFactory ) {
        super( Person.class, sessionFactory );
    }

    @Override
    protected Person findByBusinessKey( Person person ) {
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( Person.class );
        BusinessKey.addRestrictions( queryObject, person );
        return ( Person ) queryObject.uniqueResult();
    }
}